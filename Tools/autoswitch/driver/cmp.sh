#!/bin/zsh
# cmp.sh units [en]: driver lazy output vs sim --lazy dump, line by line
u=$1; st=${2:-vi}
DRV_OUT=${TMPDIR:-/tmp}/drv.txt ./drvlazy $u --start $st >/dev/null
../sim/sim $u --start $st --lazy --dump ${TMPDIR:-/tmp}/simdump.txt >/dev/null
python3 - <<'PY'
import os
d=open(os.environ.get('TMPDIR','/tmp').rstrip('/')+'/drv.txt',encoding='utf-8').read().split('\n')
s=[l.split('\t')[0] for l in open(os.environ.get('TMPDIR','/tmp').rstrip('/')+'/simdump.txt',encoding='utf-8').read().rstrip('\n').split('\n')]
import re
D=re.sub(r'[\W\d_]+','',''.join(d)); S=re.sub(r'[\W\d_]+','',''.join(s))
print('whole text equal:', D==S, len(D), len(S))
if D!=S:
    i=next(i for i,(a,b) in enumerate(zip(D,S)) if a!=b); print(' DRV', D[i-60:i+60]); print(' SIM', S[i-60:i+60])
diff=[]
print('lines', len(s), 'driver', len(d), 'differ', len(diff))
for i,a,b in diff[:3]: print(i, '\n DRV', a[:150], '\n SIM', b[:150])
PY
