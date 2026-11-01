import importlib.util
from pathlib import Path
import unittest
spec = importlib.util.spec_from_file_location('watch', Path(__file__).resolve().parents[1] / 'scripts/release_watch.py')
watch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(watch)


class ReleaseWatcherTest(unittest.TestCase):
    def test_only_completed_releases(self):
        track = {'releases': [{'status': s, 'versionCodes': [str(v)]} for s, v in
                 [('draft', 100), ('inProgress', 101), ('halted', 102), ('completed', 90)]]}
        self.assertEqual(watch.latest_completed(track), 90)
        self.assertIsNone(watch.latest_completed({'releases': []}))

    def test_first_activation_baselines_without_broadcast(self):
        sent, saved = [], []
        watch.process(None, 100, 1000, sent.append, saved.append)
        self.assertEqual(sent, [])
        self.assertEqual(saved[0]['version'], 100)

    def test_propagation_delay_and_one_new_release(self):
        sent, saved = [], []
        state = {'version': 100, 'candidate': None, 'seen_at': None}
        watch.process(state, 101, 1000, sent.append, saved.append)
        watch.process(saved[-1], 101, 1200, sent.append, saved.append)
        self.assertEqual(sent, [])
        watch.process(saved[-1], 101, 1600, sent.append, saved.append)
        self.assertEqual(len(sent), 1)
        self.assertEqual(sent[0]['message']['data']['versionCode'], '101')
        self.assertEqual(saved[-1]['version'], 101)
        watch.process(saved[-1], 101, 9999, sent.append, saved.append)
        self.assertEqual(len(sent), 1)

    def test_failure_does_not_mark_delivery(self):
        saved = []
        def fail(_):
            raise RuntimeError('FCM failed')
        state = {'version': 100, 'candidate': 101, 'seen_at': 0}
        with self.assertRaises(RuntimeError):
            watch.process(state, 101, 1000, fail, saved.append)
        self.assertEqual(saved, [])
        self.assertEqual(state['version'], 100)

    def test_halt_or_rollback_clears_pending_candidate(self):
        state = {'version': 100, 'candidate': 101, 'seen_at': 0}
        for version in (None, 100, 99):
            updated, version = watch.decision(state, version, 1000)
            self.assertIsNone(version)
            self.assertIsNone(updated['candidate'])

    def test_no_financial_data_or_url_in_push(self):
        message = watch.update_message(101)['message']
        self.assertEqual(set(message['data']), {'type', 'package', 'versionCode'})
        self.assertNotIn('notification', message)
        with self.assertRaises(ValueError):
            watch.update_message(-1)


if __name__ == '__main__':
    unittest.main()
