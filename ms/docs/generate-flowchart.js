// 生成流程图脚本
// 需要先安装: npm install -g @mermaid-js/mermaid-cli

const fs = require('fs');
const path = require('path');

// 流程图定义
const flowchartCode = `
flowchart TD
    A[开始付款处理] --> B[处理当期和过去期间]
    B --> C{判断是否跨期付款?}
    C -->|非跨期| D{超额还是不足?}
    C -->|跨期| E[记录预付分录]
    D -->|超额| F[记录借方费用<br/>无预付科目]
    D -->|不足| G[记录贷方费用]
    E --> H[逐月预付转应付]
    H --> I[最后一期特殊处理<br/>总差异调整]
    F --> J[完成]
    G --> J
    I --> J
`;

// 保存为.mmd文件
const mmdPath = path.join(__dirname, 'payment-flowchart.mmd');
fs.writeFileSync(mmdPath, flowchartCode.trim());

console.log('流程图定义已保存到:', mmdPath);
console.log('');
console.log('要生成图片，请运行以下命令:');
console.log('1. 安装mermaid-cli: npm install -g @mermaid-js/mermaid-cli');
console.log('2. 生成PNG: mmdc -i payment-flowchart.mmd -o payment-flowchart.png');
console.log('3. 转换为JPG: 使用图片编辑工具将PNG转换为JPG');
