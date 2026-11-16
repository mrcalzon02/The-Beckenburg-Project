const fs = require("node:fs");
const path = require("node:path");
const work = process.argv[2];
const validator = require(path.join(work, "package"));

async function main() {
    if (validator.version() !== "2.0.0-dev.3.10") throw new Error("Unexpected validator version");
    const files = variant => fs.readdirSync(path.join(work, variant, "models"))
        .filter(name => /\.(glb|gltf)$/.test(name)).sort();
    const names = files("before");
    if (names.length !== 21 || names.join() !== files("after").join()) throw new Error("Mismatched fixture sets");
    console.log("file\tvariant\terrors\twarnings\tinfos\thints\terror_or_warning");
    for (const name of names) {
        for (const variant of ["before", "after"]) {
            const bytes = fs.readFileSync(path.join(work, variant, "models", name));
            const report = await validator.validateBytes(bytes, {uri: name, maxIssues: 0});
            const issues = report.issues;
            const diagnoses = issues.messages.filter(message => message.severity <= 1)
                .map(message => message.code + "@" + message.pointer);
            console.log([name, variant, issues.numErrors, issues.numWarnings, issues.numInfos,
                issues.numHints, diagnoses.join(",") || "-"].join("\t"));
            if (process.env.GLTF_REPORT_DIR) {
                const directory = path.join(process.env.GLTF_REPORT_DIR, variant);
                fs.mkdirSync(directory, {recursive: true});
                fs.writeFileSync(path.join(directory, name + ".json"), JSON.stringify(report, null, 2));
            }
        }
    }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
