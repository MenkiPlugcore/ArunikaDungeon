#!/usr/bin/env python3
"""SQLite statement smoke test (stdlib; not JDBC or Paper). Test exact SQL literals from Java on a disposable legacy snapshot."""
import argparse, pathlib, sqlite3, re, uuid
p=argparse.ArgumentParser();p.add_argument('--db',required=True,type=pathlib.Path);args=p.parse_args()
source=pathlib.Path(__file__).resolve().parents[1]/'src/main/java/site/cadera/arunika/dungeon/exchange/ExchangeLedger.java'
queries=re.findall(r'"((?:SELECT|INSERT|UPDATE|DELETE|CREATE INDEX)[^"\n]+)"',source.read_text())
def sql(prefix):
    matches=[q for q in queries if q.startswith(prefix)]
    assert len(matches)==1,(prefix,matches)
    return matches[0]
def check(ok,label):
    assert ok,label
    print('PASS',label)
a,b,r,tx=[str(uuid.uuid4()) for _ in range(4)]
with sqlite3.connect(':memory:') as c,sqlite3.connect('file:'+str(args.db.absolute())+'?mode=ro',uri=True) as src:
    src.backup(c)
    base=[c.execute('SELECT COUNT(*) FROM '+t).fetchone()[0] for t in ('transactions','purchases','reward_stock')]
    check(base==[113,109,32],'archive rows preserved in disposable SQLite')
    c.execute(sql('CREATE INDEX IF NOT EXISTS adg_tx_player_status'))
    pending=sql('SELECT count(*) FROM transactions WHERE player_id=')
    stock=sql('INSERT OR IGNORE INTO reward_stock')
    decrement=sql('UPDATE reward_stock SET remaining=remaining-1')
    purchase=sql('INSERT INTO purchases')
    reserve=sql('INSERT INTO transactions')
    move=sql('UPDATE transactions SET status=')
    c.execute(stock,(r,1,1));check(c.execute(decrement,(r,)).rowcount==1,'reserve finite stock')
    c.execute(purchase,(a,r));c.execute(reserve,(tx,a,'fixture',r,'ITEM',20,'ADG_RESERVED','test'))
    check(c.execute(pending,(a,)).fetchone()[0]==1,'pending transaction index query detects lock')
    check(c.execute(decrement,(r,)).rowcount==0,'cannot oversell last stock')
    check(c.execute(move,('ADG_CANCELLED','verified',tx,a,r,20,'ADG_RESERVED')).rowcount==1,'cancel transition')
    c.execute(sql('UPDATE reward_stock SET remaining=remaining+1'),(r,));c.execute(sql('UPDATE purchases SET count=count-1'),(a,r));c.execute(sql('DELETE FROM purchases'),(a,r))
    check(c.execute('SELECT remaining FROM reward_stock WHERE reward_id=?',(r,)).fetchone()[0]==1,'cancel restores stock')
    check(c.execute(sql('UPDATE reward_stock SET remaining=remaining+1'),(r,)).rowcount==0,'stock restoration cannot exceed configured cap')
    check(c.execute(sql('UPDATE purchases SET count=count-1'),(a,r)).rowcount==0,'missing purchase cannot be decremented')
    unlimited=str(uuid.uuid4());c.execute(stock,(unlimited,-1,-1));check(c.execute(sql('SELECT count(*) FROM reward_stock WHERE reward_id=? AND configured=-1'),(unlimited,)).fetchone()[0]==1,'unlimited stock recognized without increment')
    check(c.execute(pending,(a,)).fetchone()[0]==0,'cancel clears pending lock')
    check(c.execute(move,('ADG_CANCELLED','duplicate',tx,a,r,20,'ADG_RESERVED')).rowcount==0,'double cancel rejected by state condition')
    tx2=str(uuid.uuid4());c.execute(decrement,(r,));c.execute(purchase,(b,r));c.execute(reserve,(tx2,b,'fixture2',r,'ITEM',20,'ADG_RESERVED','test'))
    check(c.execute(move,('ADG_DEBITED','saved',tx2,b,r,20,'ADG_RESERVED')).rowcount==1,'debit transition')
    check(c.execute(move,('ADG_CANCELLED','invalid',tx2,b,r,20,'ADG_RESERVED')).rowcount==0,'cannot cancel after debit')
    check(c.execute(move,('DELIVERED','done',tx2,b,r,20,'ADG_DEBITED')).rowcount==1,'delivery transition')
    check(c.execute(move,('DELIVERED','again',tx2,b,r,20,'ADG_DEBITED')).rowcount==0,'double delivery rejected by state condition')
    check(c.execute('PRAGMA integrity_check').fetchone()==('ok',),'SQLite integrity after transactions')
