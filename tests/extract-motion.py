from pathlib import Path
import re,sys
workflow=Path('.github/workflows/build.yml').read_text()
source=re.search(r"cat > \$D/CaptureMotion.java <<'EOF'\n(.*?)\n          EOF",workflow,re.S).group(1)
Path(sys.argv[1]).write_text('\n'.join(line[10:] if line.startswith('          ') else line for line in source.splitlines()))
