#!/usr/bin/env python3
import datetime, json, os, re, urllib.request

repo = os.environ["GITHUB_REPOSITORY"]
token = os.environ["GITHUB_TOKEN"]
headers = {"Authorization": "Bearer " + token, "Accept": "application/vnd.github+json", "User-Agent": "beckenburg-import-diagnostics", "X-GitHub-Api-Version": "2022-11-28"}

def get(path):
    req = urllib.request.Request("https://api.github.com/repos/" + repo + "/" + path, headers=headers)
    with urllib.request.urlopen(req, timeout=30) as res: return res.read().decode("utf-8", errors="replace")

def scrub(s):
    s = s.replace(token, "[REDACTED]")
    s = re.sub(r"(?i)(bearer\s+)[^\s\"']+", r"\1[REDACTED]", s)
    return re.sub(r"(gh[pousr]_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,})", "[REDACTED]", s)

def main():
    data = json.loads(get("actions/workflows/import-beckenburg.yml/runs?per_page=20"))
    failures = sorted([r for r in data["workflow_runs"] if r.get("conclusion") == "failure" and r.get("event") == "push"],key=lambda r:r.get("run_number",0),reverse=True)
    if not failures: raise RuntimeError("Failed import workflow runs not found")
    lines = ["Verified import failures from GitHub Actions, UTC "+datetime.datetime.now(datetime.timezone.utc).isoformat(),""]
    for run in failures[:2]:
        rid = run["id"]
        lines += ["RUN: "+run.get("html_url",""),"SHA: "+run.get("head_sha",""),"Conclusion: "+str(run.get("conclusion"))]
        jobs = json.loads(get("actions/runs/"+str(rid)+"/jobs?per_page=100"))["jobs"]
        for job in jobs:
            lines += ["JOB: "+str(job.get("name"))+"; "+str(job.get("conclusion"))]
            for step in job.get("steps") or []: lines.append("STEP: "+str(step.get("conclusion"))+" "+str(step.get("name")))
            if job.get("conclusion") == "failure":
                try:
                    log = scrub(get("actions/jobs/"+str(job["id"])+"/logs"))
                    logs = log.splitlines()
                    markers=[x for x in logs if any(t in x.lower() for t in ["::error::","error:","fatal:","traceback","assertionerror","permission denied"])][-14:]
                    lines += ["ERROR MARKERS:",*markers,"LOG END:",*logs[-75:],"END LOG"]
                except Exception as e: lines += ["LOG FETCH ERROR: "+scrub(str(e))]
        lines.append("---")
    body="\n".join(lines)[:55000]
    print(body[:14000])
    payload=json.dumps({"body":body}).encode()
    req=urllib.request.Request("https://api.github.com/repos/"+repo+"/issues/1/comments",data=payload,headers={**headers,"Content-Type":"application/json"},method="POST")
    with urllib.request.urlopen(req,timeout=30) as r: print("COMMENT CREATED:",json.load(r).get("html_url"))

if __name__ == "__main__": main()
