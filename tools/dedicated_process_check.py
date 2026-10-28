from pathlib import Path
import subprocess,tempfile,socket,time,json
import os,shutil
r=Path(__file__).resolve().parents[1]
jdk=Path(os.environ['JAVA_HOME'])/'bin' if 'JAVA_HOME' in os.environ else Path(shutil.which('javac')).resolve().parent
java=str(jdk/('java.exe' if os.name=='nt' else 'java'))
with tempfile.TemporaryDirectory(prefix='beckenburg-dedicated-') as tmp:
    subprocess.run([java,'-cp','build/dedicated-server.jar','hbnet.Setup',tmp],cwd=r,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,check=True,timeout=30)
    with socket.socket() as bound:bound.bind(('127.0.0.1',0));port=bound.getsockname()[1]
    output=Path(tmp)/'process.log'
    with output.open('w') as log:
        host=subprocess.Popen([java,'-Djava.awt.headless=true','-jar','build/dedicated-server.jar',tmp,str(port),'1','127.0.0.1','HOUSE'],cwd=r,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True)
        try:
            end=time.monotonic()+10
            while time.monotonic()<end:
                if 'Private headless case ready' in output.read_text(errors='replace'):break
                if host.poll() is not None:raise RuntimeError('Headless startup failed')
                time.sleep(.05)
            else:raise RuntimeError('Headless startup timeout')
            host.stdin.write('status\nstop\n');host.stdin.flush();host.wait(timeout=10)
            assert host.returncode==0
            with socket.socket() as released:released.bind(('127.0.0.1',port))
            result={'game':'beckenburg','mode':'real standalone secure host lifecycle, no player/TLS handshake claim','server_exit':0,'port_released':True,'status_no_phantom_seats':'Players 0;' in output.read_text()}
            assert result['status_no_phantom_seats'];(r/'build/network-checks/dedicated-process.json').write_text(json.dumps(result,indent=2));print(json.dumps(result))
        finally:
            if host.poll() is None:host.kill();host.wait()
