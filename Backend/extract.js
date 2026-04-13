const fs = require('fs');
const path = require('path');

function walk(d) {
    let r = [];
    fs.readdirSync(d).forEach(f => {
        let p = path.join(d, f);
        if (fs.statSync(p).isDirectory()) r = r.concat(walk(p));
        else if (f.endsWith('.java')) r.push(p);
    });
    return r;
}

let cFiles = walk('src/main/java/com/example/smartAttendence/controller/v1');
let endpoints = [];
cFiles.forEach(f => {
    let content = fs.readFileSync(f, 'utf8');
    let baseMatch = /@RequestMapping\(\s*"([^"]+)"\s*\)/.exec(content);
    let base = baseMatch ? baseMatch[1] : '';
    
    // Quick regex to find all methods
    let regex = /@(Get|Post|Put|Delete|Patch)Mapping\(\s*"([^"]*)"\s*\)[\s\S]*?public\s+([^\s]+)\s+([^\(]+)\s*\(([^)]*)\)/g;
    let match;
    while ((match = regex.exec(content)) !== null) {
        let method = match[1].toUpperCase();
        let url = base + match[2];
        let returnType = match[3];
        let funcName = match[4];
        let args = match[5];
        
        let payload = "-";
        let bodyMatch = /@RequestBody\s+([A-Za-z0-9_<>]+)/.exec(args);
        if (bodyMatch) {
            payload = bodyMatch[1];
        }
        
        endpoints.push(`| **${path.basename(f).replace('Controller.java', '')}** | \`${method}\` | \`${url}\` | \`${payload}\` | \`${returnType}\` |`);
    }
});

fs.writeFileSync('endpoints.md', endpoints.join('\n'));
console.log("Endpoints written to endpoints.md");
