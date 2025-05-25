#!/usr/bin/env node

/**
 * API Documentation Generator
 * 
 * This script parses the TypeScript API files and generates markdown documentation.
 * It extracts:
 * - JSDoc comments
 * - Function signatures
 * - Interface definitions
 * - API endpoints
 */

const fs = require('fs');
const path = require('path');
const glob = require('glob');

const API_DIR = path.resolve(__dirname, '../src/lib/api');
const DOCS_DIR = path.resolve(__dirname, '../docs/api');

// Ensure docs directory exists
if (!fs.existsSync(DOCS_DIR)) {
  fs.mkdirSync(DOCS_DIR, { recursive: true });
}

/**
 * Extract JSDoc comments from a file
 */
function extractJSDocComments(content) {
  const comments = [];
  const regex = /\/\*\*\s*([\s\S]*?)\s*\*\//g;
  let match;

  while ((match = regex.exec(content)) !== null) {
    const comment = match[1]
      .trim()
      .replace(/\s*\*\s*/g, '\n')
      .trim();
    comments.push({
      comment,
      position: match.index
    });
  }

  return comments;
}

/**
 * Extract interface definitions
 */
function extractInterfaces(content) {
  const interfaces = [];
  const regex = /export\s+interface\s+(\w+)([^{]*){\s*([\s\S]*?)\s*}/g;
  let match;

  while ((match = regex.exec(content)) !== null) {
    interfaces.push({
      name: match[1],
      generics: match[2].trim(),
      properties: match[3].trim(),
      position: match.index
    });
  }

  return interfaces;
}

/**
 * Extract function definitions
 */
function extractFunctions(content) {
  const functions = [];
  const regex = /export\s+const\s+(\w+)\s*=\s*(\([^)]*\))\s*=>\s*{/g;
  let match;

  while ((match = regex.exec(content)) !== null) {
    functions.push({
      name: match[1],
      params: match[2].trim(),
      position: match.index
    });
  }

  return functions;
}

/**
 * Extract API endpoints
 */
function extractEndpoints(content) {
  const endpoints = [];
  const regex = /('|"|`)\/api\/[^'"`]+('|"|`)/g;
  let match;

  while ((match = regex.exec(content)) !== null) {
    const endpoint = match[0].replace(/['"`]/g, '');
    if (!endpoints.includes(endpoint)) {
      endpoints.push(endpoint);
    }
  }

  return endpoints;
}

/**
 * Generate documentation for a file
 */
function generateFileDoc(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const relativePath = path.relative(API_DIR, filePath);
  const fileName = path.basename(filePath);
  const moduleName = path.basename(filePath, path.extname(filePath));
  
  // Extract components
  const comments = extractJSDocComments(content);
  const interfaces = extractInterfaces(content);
  const functions = extractFunctions(content);
  const endpoints = extractEndpoints(content);
  
  // Find module description (first comment block)
  const moduleDesc = comments.length > 0 ? comments[0].comment : `${moduleName} API module`;
  
  // Start building markdown
  let markdown = `# ${moduleName} API\n\n`;
  markdown += `${moduleDesc}\n\n`;
  markdown += `**File**: \`${relativePath}\`\n\n`;
  
  // Add interface documentation
  if (interfaces.length > 0) {
    markdown += `## Types\n\n`;
    
    interfaces.forEach(iface => {
      markdown += `### ${iface.name}${iface.generics}\n\n`;
      
      // Find comment for this interface
      const ifaceComment = comments.find(c => c.position < iface.position && 
        !comments.some(c2 => c2.position > c.position && c2.position < iface.position));
      
      if (ifaceComment) {
        markdown += `${ifaceComment.comment}\n\n`;
      }
      
      markdown += "```typescript\n";
      markdown += `interface ${iface.name}${iface.generics} {\n  ${iface.properties.replace(/\n/g, '\n  ')}\n}\n`;
      markdown += "```\n\n";
    });
  }
  
  // Add function documentation
  if (functions.length > 0) {
    markdown += `## Functions\n\n`;
    
    functions.forEach(func => {
      markdown += `### ${func.name}\n\n`;
      
      // Find comment for this function
      const funcComment = comments.find(c => c.position < func.position && 
        !comments.some(c2 => c2.position > c.position && c2.position < func.position));
      
      if (funcComment) {
        markdown += `${funcComment.comment}\n\n`;
      }
      
      markdown += "```typescript\n";
      markdown += `const ${func.name} = ${func.params} => { ... }\n`;
      markdown += "```\n\n";
    });
  }
  
  // Add endpoints documentation
  if (endpoints.length > 0) {
    markdown += `## API Endpoints\n\n`;
    endpoints.forEach(endpoint => {
      markdown += `- \`${endpoint}\`\n`;
    });
    markdown += "\n";
  }
  
  return markdown;
}

/**
 * Generate documentation for all API files
 */
function generateAllDocs() {
  // V1 API docs
  const v1Files = glob.sync(`${API_DIR}/!(v1|v2)/*.ts`);
  if (v1Files.length > 0) {
    const v1Dir = path.join(DOCS_DIR, 'v1');
    if (!fs.existsSync(v1Dir)) {
      fs.mkdirSync(v1Dir, { recursive: true });
    }
    
    v1Files.forEach(filePath => {
      const moduleName = path.basename(filePath, '.ts');
      const markdown = generateFileDoc(filePath);
      fs.writeFileSync(path.join(v1Dir, `${moduleName}.md`), markdown);
    });
    
    // Generate index
    let v1Index = '# API v1 Documentation\n\n';
    v1Index += 'First-generation API modules\n\n';
    v1Index += '## Modules\n\n';
    
    v1Files.forEach(filePath => {
      const moduleName = path.basename(filePath, '.ts');
      v1Index += `- [${moduleName}](./v1/${moduleName}.md)\n`;
    });
    
    fs.writeFileSync(path.join(DOCS_DIR, 'v1.md'), v1Index);
  }
  
  // V2 API docs
  const v2Files = glob.sync(`${API_DIR}/v2/*.ts`);
  if (v2Files.length > 0) {
    const v2Dir = path.join(DOCS_DIR, 'v2');
    if (!fs.existsSync(v2Dir)) {
      fs.mkdirSync(v2Dir, { recursive: true });
    }
    
    v2Files.forEach(filePath => {
      const moduleName = path.basename(filePath, '.ts');
      if (moduleName === 'index') return; // Skip index file
      
      const markdown = generateFileDoc(filePath);
      fs.writeFileSync(path.join(v2Dir, `${moduleName}.md`), markdown);
    });
    
    // Generate index
    let v2Index = '# API v2 Documentation\n\n';
    v2Index += 'Second-generation API modules with improved patterns\n\n';
    v2Index += '## Modules\n\n';
    
    v2Files.forEach(filePath => {
      const moduleName = path.basename(filePath, '.ts');
      if (moduleName === 'index') return; // Skip index file
      v2Index += `- [${moduleName}](./v2/${moduleName}.md)\n`;
    });
    
    fs.writeFileSync(path.join(DOCS_DIR, 'v2.md'), v2Index);
  }
  
  // Main index
  let mainIndex = '# DataFlare UI API Documentation\n\n';
  mainIndex += 'This documentation is automatically generated from the source code.\n\n';
  
  if (v1Files.length > 0) {
    mainIndex += '## [API v1](./v1.md)\n\n';
    mainIndex += 'First-generation API modules\n\n';
  }
  
  if (v2Files.length > 0) {
    mainIndex += '## [API v2](./v2.md)\n\n';
    mainIndex += 'Second-generation API modules with improved patterns\n\n';
  }
  
  fs.writeFileSync(path.join(DOCS_DIR, 'index.md'), mainIndex);
  
  console.log(`API documentation generated in ${DOCS_DIR}`);
}

// Run the generator
generateAllDocs();

// Export functions for testing
if (typeof jest !== 'undefined') {
  module.exports = {
    extractJSDocComments,
    extractInterfaces,
    extractFunctions,
    extractEndpoints,
    generateFileDoc,
    generateAllDocs,
    __getMarkdownForTesting: (filePath) => {
      // For the test, we just want to generate the markdown without actually reading the file
      // The test will mock fs.readFileSync to return the content it wants
      return generateFileDoc(filePath);
    }
  };
} 