import importlib.util, pathlib, unittest, tempfile, zipfile, hashlib
spec=importlib.util.spec_from_file_location('smoke',pathlib.Path(__file__).with_name('smoke-paper.py'));smoke=importlib.util.module_from_spec(spec);spec.loader.exec_module(smoke)
class SmokeTests(unittest.TestCase):
 def test_status(self):
  m=smoke.LogMonitor();m.add('\x1b[32m'+smoke.STATUS+'\x1b[0m');self.assertTrue(m.contains(smoke.STATUS));self.assertFalse(m.contains(smoke.STATUS,1));m.check()
 def test_fatal(self):
  for s in ('Foundation disabled','Shutdown failed: GUI','java.lang.NoSuchMethodError: provider','Foundation DB failed: missing'):
   m=smoke.LogMonitor();m.add(s);self.assertRaises(smoke.SmokeError,m.check)
 def test_expected_exchange_lock_not_fatal(self):
  m=smoke.LogMonitor();m.add('Exchange unavailable: no legacy data');m.check()
 def test_input_missing_hash(self):
  with tempfile.TemporaryDirectory() as d:
   f=pathlib.Path(d)/f'ArunikaDungeon-{smoke.VERSION}.jar';f.write_bytes(b'invalid');self.assertRaises(smoke.SmokeError,smoke.verify_input,f)
 def test_input_hash_and_manifest(self):
  with tempfile.TemporaryDirectory() as d:
   f=pathlib.Path(d)/f'ArunikaDungeon-{smoke.VERSION}.jar';h=f.with_name(f.name+'.sha256')
   with zipfile.ZipFile(f,'w') as z:z.writestr('plugin.yml',"version: 'wrong'\n")
   h.write_text(smoke.digest(f)+'  '+f.name);self.assertRaises(smoke.SmokeError,smoke.verify_input,f)
   h.write_text('0'*64+'  '+f.name);self.assertRaises(smoke.SmokeError,smoke.verify_input,f)
if __name__=='__main__': unittest.main(verbosity=2)
