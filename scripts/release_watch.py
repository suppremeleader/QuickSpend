"""Watch completed internal Play releases; send version-only update alerts via FCM.

No APK/AAB is built or published. Google access uses ADC from Workload Identity
Federation. Version bookkeeping goes to a separate GitHub branch, never main.
"""
import base64
import json
import os
import re
import time
from urllib import request, error, parse

PACKAGE = 'com.quickspend.app'
TRACK = 'internal'
TOPIC = 'quickspend_internal_updates'
REPOSITORY = 'suppremeleader/QuickSpend'
STATE_BRANCH = 'release-notification-state'
STATE_PATH = 'release-state/internal.json'
MAX_VERSION = 2100000000


def latest_completed(track):
    versions = []
    for release in track.get('releases', []):
        if release.get('status') != 'completed':
            continue
        codes = release.get('versionCodes', [])
        if not isinstance(codes, list):
            raise ValueError('Unexpected Play version list')
        for value in codes:
            if re.fullmatch(r'[1-9][0-9]{0,9}', str(value)):
                version = int(value)
                if version <= MAX_VERSION:
                    versions.append(version)
    return max(versions) if versions else None


def decision(state, version, now, propagation_wait=600):
    """First activation baselines existing releases; only later releases alert."""
    if version is None:
        # A formerly observed candidate may now be halted or removed.
        if state and state.get('candidate'):
            state = {**state, 'candidate': None, 'seen_at': None}
        return state, None
    if state is None:
        return {'version': version, 'candidate': None, 'seen_at': None}, None
    state = dict(state)
    if version <= state.get('version', 0):
        state.update(candidate=None, seen_at=None)
        return state, None
    if state.get('candidate') != version:
        state.update(candidate=version, seen_at=now)
        return state, None
    if now - state.get('seen_at', now) < propagation_wait:
        return state, None
    return state, version


def update_message(version):
    if not isinstance(version, int) or not 0 < version <= MAX_VERSION:
        raise ValueError('Invalid version code')
    return {'message': {'topic': TOPIC, 'data': {'type': 'app_update',
            'package': PACKAGE, 'versionCode': str(version)},
            'android': {'priority': 'HIGH', 'ttl': '86400s',
                        'collapse_key': 'quickspend_update'}}}


def process(state, version, now, send, save):
    updated, announce = decision(state, version, now)
    if announce is not None:
        # If this succeeds and state saving fails, a retry is possible. Clients
        # deduplicate by version. Never mark a failed FCM send as delivered.
        send(update_message(announce))
        updated = {'version': announce, 'candidate': None, 'seen_at': None}
    if updated != state:
        save(updated)
    return announce


def http_json(method, url, token, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = request.Request(url, data=data, method=method, headers={
        'Authorization': 'Bearer ' + token, 'Accept': 'application/json',
        'Content-Type': 'application/json'
    })
    try:
        with request.urlopen(req, timeout=30) as response:
            content = response.read()
            return json.loads(content) if content else {}
    except error.HTTPError as exc:
        # Do not log credentials, response bodies or the full authenticated request.
        raise RuntimeError(f'{parse.urlparse(url).hostname}: HTTP {exc.code}') from None


def completed_track(google_token):
    base = f'https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}/edits'
    edit = http_json('POST', base, google_token, {})
    edit_id = edit.get('id')
    if not isinstance(edit_id, str) or not re.fullmatch(r'[A-Za-z0-9_-]+', edit_id):
        raise RuntimeError('Invalid Play edit identifier')
    try:
        return http_json('GET', f'{base}/{edit_id}/tracks/{TRACK}', google_token)
    finally:
        # The read snapshot is always discarded. There is no edits.commit call.
        http_json('DELETE', f'{base}/{edit_id}', google_token)


class GithubState:
    def __init__(self, token):
        self.token, self.sha = token, None
        self.base = f'https://api.github.com/repos/{REPOSITORY}'

    def load(self):
        req = request.Request(f'{self.base}/contents/{STATE_PATH}?ref={STATE_BRANCH}',
                              headers={'Authorization': 'Bearer ' + self.token,
                                       'Accept': 'application/vnd.github+json'})
        try:
            with request.urlopen(req, timeout=30) as response:
                value = json.load(response)
        except error.HTTPError as exc:
            if exc.code == 404:
                return None
            raise RuntimeError(f'GitHub state read: HTTP {exc.code}') from None
        self.sha = value['sha']
        data = json.loads(base64.b64decode(value['content']))
        if not isinstance(data, dict) or not isinstance(data.get('version'), int):
            raise RuntimeError('Invalid release notification state; refusing to notify')
        return data

    def ensure_branch(self):
        url = f'{self.base}/git/ref/heads/{STATE_BRANCH}'
        req = request.Request(url, headers={'Authorization': 'Bearer ' + self.token})
        try:
            with request.urlopen(req, timeout=30):
                return
        except error.HTTPError as exc:
            if exc.code != 404:
                raise RuntimeError(f'GitHub state branch: HTTP {exc.code}') from None
        head = http_json('GET', f'{self.base}/git/ref/heads/main', self.token)
        http_json('POST', f'{self.base}/git/refs', self.token,
                  {'ref': 'refs/heads/' + STATE_BRANCH, 'sha': head['object']['sha']})

    def save(self, state):
        self.ensure_branch()
        body = {'message': 'Record internal release notification state',
                'branch': STATE_BRANCH,
                'content': base64.b64encode((json.dumps(state) + '\n').encode()).decode()}
        if self.sha:
            body['sha'] = self.sha
        result = http_json('PUT', f'{self.base}/contents/{STATE_PATH}', self.token, body)
        self.sha = result['content']['sha']


def main():
    if os.environ.get('UPDATE_ALERTS_ENABLED') != 'true':
        print('Release alerts are disabled; no network actions performed.')
        return
    if os.environ.get('GITHUB_REPOSITORY') != REPOSITORY:
        raise RuntimeError('Unexpected repository')
    config = json.loads(open('config/google-services.json', encoding='utf-8').read())
    project = config['project_info']['project_id']
    if not re.fullmatch(r'[a-z][a-z0-9-]{3,62}', project):
        raise RuntimeError('Invalid Firebase project')
    import google.auth
    from google.auth.transport.requests import Request
    credentials, _ = google.auth.default(scopes=[
        'https://www.googleapis.com/auth/androidpublisher',
        'https://www.googleapis.com/auth/firebase.messaging'])
    credentials.refresh(Request())
    token = credentials.token
    state = GithubState(os.environ['GH_TOKEN'])
    version = latest_completed(completed_track(token))
    announced = process(state.load(), version, int(time.time()),
                        lambda message: http_json('POST',
                            f'https://fcm.googleapis.com/v1/projects/{project}/messages:send',
                            token, message), state.save)
    print('Release notification sent.' if announced else 'No newly eligible release notification.')


if __name__ == '__main__':
    main()
