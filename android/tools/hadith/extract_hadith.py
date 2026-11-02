import json,re,sys
D='[ً-ْٰـ]*'
def P(s):  # pattern tolerant to diacritics; spaces flexible; hamza variants
    out=''
    for ch in s:
        if ch==' ': out+=r'\s*'
        elif ch in 'اأإآ': out+='[اأإآ]'+D
        elif ch in 'يى': out+='[يى]'+D
        elif ch in 'هة': out+='[هة]'+D
        else: out+=re.escape(ch)+D
    return out
HON=r'\(?\s*(?:'+'|'.join([P('عليه السلام'),P('عليهما السلام'),P('صلوات الله عليه'),P('صلى الله عليه وآله'),P('صلى الله عليه و آله'),P('صلى الله عليه')])+r')\s*\)?'
SPK=[ # order matters
 ('أبي جعفر الثاني','الإمام الجواد (ع)'),('أبي الحسن الرضا','الإمام الرضا (ع)'),('أبي الحسن الثاني','الإمام الرضا (ع)'),('الرضا','الإمام الرضا (ع)'),
 ('أبي الحسن الأول','الإمام الكاظم (ع)'),('أبي الحسن موسى','الإمام الكاظم (ع)'),('العبد الصالح','الإمام الكاظم (ع)'),
 ('أبي عبد الله','الإمام الصادق (ع)'),('جعفر بن محمد','الإمام الصادق (ع)'),('أبي جعفر','الإمام الباقر (ع)'),
 ('علي بن الحسين','الإمام السجاد (ع)'),('أمير المؤمنين','الإمام علي (ع)'),('رسول الله','النبي (ص)'),('النبي','النبي (ص)'),('علي','الإمام علي (ع)')]
QAL=r'\s*'+P('قال')+r'\s*:?\s*'
INNER=[(P('قال رسول الله')+r'\s*'+HON+r'\s*:?\s*','النبي (ص)'),(P('قال النبي')+r'\s*'+HON+r'\s*:?\s*','النبي (ص)'),(P('قال أمير المؤمنين')+r'\s*'+HON+r'\s*:?\s*','الإمام علي (ع)')]
BAD=re.compile('|'.join(P(w) for w in ['قلت','فقال','سألت','سأل','قال لي','قال له','قالت','فقلت','قيل','حدثني','أخبرني','عن ','قال']))
def matn(text):
    best=None
    for name,lab in SPK:
        m=re.search(P(name)+r'\s*'+HON+QAL,text)
        if m and (best is None or m.start()<best[0]): best=(m.start(),m.end(),lab)
    if not best: return None
    rest=text[best[1]:].strip(); lab=best[2]
    for pat,l2 in INNER:
        m=re.match(pat,rest)
        if m: rest=rest[m.end():].strip(); lab=l2; break
    for _ in range(2):
        rest=re.sub('^(?:'+'|'.join(P(w) for w in ['سمعته يقول','سمعته','في حديث','في خطبته','في وصيته','يقول'])+r')\s*:?\s*','',rest.strip())
    rest=re.sub(r'^[«"“:\s]+','',rest); rest=re.sub(r'[»"”\s]+$','',rest)
    rest=re.sub(r'\s+',' ',rest)
    return lab,rest
SENS=re.compile('|'.join(['جاري','الزنا','زنى','زان','سندي','كرد','زنج','بربر','خوز','نبك','حيض','حائض','طلاق','طلق','نكاح','جماع','مملوك','بمملوك','يغسل','غسل','الوضوء','المكاري','يتمون','يبيض','ناصب','مرجى','الدم','بول','غائط','عورة','الشعر','حمار','كلب','خنزير','قتل','يقتل','الجن','السحر','المرأة','امرأة','امرأت','النساء','الصبي','وطئ','الهبة','النحل','للامام','فللامام','صلى الله يوم','النعموما','ألق']))
OKG=('صحيح','حسن','موثق','معتبر')
def plain(s): return re.sub('[ً-ْٰـ]','',s)
out=[]
BOOKS=[('Al-Kafi-Volume-1-Kulayni.json','الكافي'),('Al-Kafi-Volume-2-Kulayni.json','الكافي'),('Mujam-al-Ahadith-al-Mutabara-Muhsini.json','معجم الأحاديث المعتبرة'),
 ('Al-Amali-Saduq.json','أمالي الصدوق'),('Al-Khisal-Saduq.json','الخصال'),('Thawab-al-Amal-wa-iqab-al-Amal-Saduq.json','ثواب الأعمال'),('Kitab-al-Mumin-Ahwazi.json','كتاب المؤمن'),
 ('Sifat-al-Shia-Saduq.json','صفات الشيعة'),('Kitab-al-Zuhd-Ahwazi.json','كتاب الزهد'),('Uyun-akhbar-al-Rida-Volume-1-Saduq.json','عيون أخبار الرضا'),('Uyun-akhbar-al-Rida-Volume-2-Saduq.json','عيون أخبار الرضا'),('Al-Amali-Mufid.json','أمالي المفيد'),('Maani-al-Akhbar-Saduq.json','معاني الأخبار')]
GRADED=('الكافي','معجم الأحاديث المعتبرة')
for f,book in BOOKS:
    for x in json.load(open(f)):
        if x.get('category') in ('The Book about people with Divine Authority','The Book on Oneness of Allah (God)','Introduction','Book of Narrators','Book of Principles of Jurisprudence','The Book of Divine Unity'): continue
        gtxt=(x.get('majlisiGrading') or '')+' '+(x.get('mohseniGrading') or '')
        m=re.search(r'(?:Majlisi|Mohseni):\s*([^\n]+)',gtxt)
        g=(m.group(1).strip() if m else '')
        if book in GRADED:
            if not g.startswith(OKG): continue
            if 'ضعيف' in g or 'مجهول' in g: continue
        else: g=''
        r=matn(x['arabicText'])
        if not r: continue
        lab,t=r
        p=plain(t)
        if not (25<=len(p)<=200): continue
        if SENS.search(plain(t)): continue
        if BAD.search(t) or re.search(HON,t) or '[' in t or '(' in t: continue
        if not re.search(r'[.؟!]$',t): t=t.rstrip('،,;؛')+'.'
        bk=book
        if book=='معجم الأحاديث المعتبرة':
            mm=re.match(r'\[[^\]]*\]\s*([^:]{2,30}):',x['arabicText'])
            src=mm.group(1).strip() if mm else ''
            src=re.sub('^'+P('اصول الكافي')+'$','الكافي',src)
            bk=(src+' · ' if src else '')+'معجم الأحاديث المعتبرة'
        out.append({'s':lab,'t':t,'b':bk,'g':(g.split()[0] if g else ''),'u':x.get('URL','')})
res=[];sets=[]
for o in out:
    w=set(plain(o['t']).replace('.',' ').split())
    if any(len(w&q)/max(1,len(w|q))>0.6 for q in sets): continue
    sets.append(w);res.append(o)
import collections
print(len(res),collections.Counter(o['s'] for o in res),collections.Counter(o['b'] for o in res),file=sys.stderr)
json.dump(res,open(sys.argv[1],'w',encoding='utf-8'),ensure_ascii=False,indent=0)
