"""يصنع index.html لتطبيق أندرويد من نسخة الويب (صفحة Claude).

الاستعمال:
  python3 tools/make_android_html.py WEB.html CURRENT_ANDROID.html OUT.html 3.2.1

يأخذ التطبيق كاملاً من نسخة الويب، ويضيف إليه الأجزاء الخاصة بأندرويد من النسخة الحالية:
جسر الإشعارات في الرأس، وفهرس النصوص الخفيف (النصوص الكاملة في libtexts.json وqdata.json)،
وكتلة الأذان والتحديث في آخر الملف. ثم يضبط رقم الإصدار.
"""
import re
import sys
web_path, apk_html_path, out_path, version = sys.argv[1:5]
w = open(web_path, encoding='utf-8').read().split('\n')
b = open(apk_html_path, encoding='utf-8').read().split('\n')

assert w[0].startswith('<!doctype html>') and b[0].startswith('<!doctype html>')
assert w[2].startswith('<link rel="preconnect"') and w[3].startswith('<link href="https://fonts.googleapis.com')
lib = [i for i, l in enumerate(w) if l.startswith('<script type="application/json" id="libdata">')]
qd = [i for i, l in enumerate(w) if l.startswith('<script type="application/json" id="qdata">')]
assert len(lib) == 1 and len(qd) == 1 and qd[0] == lib[0] + 1
blib = [i for i, l in enumerate(b) if l.startswith('<script type="application/json" id="libdata">')]
assert len(blib) == 1 and b[blib[0] + 1].startswith('<script type="application/json" id="qdata">')
start = [i for i, l in enumerate(b) if l.startswith('/* ---------- خاص بتطبيق أندرويد')]
end = [i for i, l in enumerate(b) if l == 'render();']
assert len(start) == 1 and len(end) == 1
android_block = b[start[0]:end[0]]

out = b[0:2] + [w[1]] + w[4:lib[0]] + b[blib[0]:blib[0] + 2] + w[qd[0] + 1:]
s = '\n'.join(out)
tail = '\nrender();\nboot();\n})();'
assert s.count(tail) == 1
s = s.replace(tail, '\n' + '\n'.join(android_block) + tail)
s, n = re.subn(r"var APP_VERSION='[\d.]+';", "var APP_VERSION='%s';" % version, s)
assert n == 1
if not s.endswith('\n'):
    s += '\n'
open(out_path, 'w', encoding='utf-8').write(s)
print('ok', len(s))
