// 生成PDF流程图脚本
// 需要先安装: npm install -g @mermaid-js/mermaid-cli

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

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

// 配置文件内容
const configContent = `{
  "theme": "default",
  "width": 1200,
  "height": 800,
  "backgroundColor": "white"
}`;

async function generatePDF() {
    try {
        // 保存流程图定义
        const mmdPath = path.join(__dirname, 'payment-flowchart.mmd');
        fs.writeFileSync(mmdPath, flowchartCode.trim());
        console.log('✅ 流程图定义已保存到:', mmdPath);

        // 保存配置文件
        const configPath = path.join(__dirname, 'mermaid-config.json');
        fs.writeFileSync(configPath, configContent);
        console.log('✅ 配置文件已保存到:', configPath);

        // 检查是否安装了mermaid-cli
        try {
            execSync('mmdc --version', { stdio: 'ignore' });
            console.log('✅ mermaid-cli 已安装');
        } catch (error) {
            console.log('❌ mermaid-cli 未安装，请先运行: npm install -g @mermaid-js/mermaid-cli');
            return;
        }

        // 生成PDF
        const pdfPath = path.join(__dirname, 'payment-flowchart.pdf');
        const command = `mmdc -i "${mmdPath}" -o "${pdfPath}" -c "${configPath}" -f pdf`;
        
        console.log('🚀 正在生成PDF...');
        execSync(command, { stdio: 'inherit' });
        console.log('✅ PDF已生成:', pdfPath);

    } catch (error) {
        console.error('❌ 生成PDF时出错:', error.message);
        console.log('');
        console.log('手动生成步骤:');
        console.log('1. 安装mermaid-cli: npm install -g @mermaid-js/mermaid-cli');
        console.log('2. 生成PDF: mmdc -i payment-flowchart.mmd -o payment-flowchart.pdf -f pdf');
    }
}

// 运行生成函数
generatePDF();
