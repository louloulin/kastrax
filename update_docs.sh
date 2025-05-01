#!/bin/bash

# Find all .mdx files containing "mastra" and replace with "kastrax"
find kastrax-doc/src/content/en -type f -name "*.mdx" | xargs grep -l "mastra" | while read file; do
    # Skip files we've already processed
    if [[ "$file" == *"overview"* ]] || [[ "$file" == *"index"* ]] || [[ "$file" == *"installation"* ]] || \
       [[ "$file" == *"first-agent"* ]] || [[ "$file" == *"architectures"* ]] || [[ "$file" == *"working-memory"* ]] || \
       [[ "$file" == *"semantic-recall"* ]] || [[ "$file" == *"memory-processors"* ]] || [[ "$file" == *"implementations"* ]] || \
       [[ "$file" == *"kastrax-dev"* ]]; then
        continue
    fi
    
    echo "Processing $file"
    
    # Create a temporary file
    temp_file="${file}.tmp"
    
    # Replace "mastra" with "kastrax" and "Mastra" with "Kastrax"
    sed 's/mastra/kastrax/g; s/Mastra/Kastrax/g' "$file" > "$temp_file"
    
    # Add implementation markers
    sed -i '' 's/^# \(.*\)$/# \1 ✅/' "$temp_file"
    sed -i '' 's/^## \(.*\)$/## \1 ✅/' "$temp_file"
    
    # Replace the original file
    mv "$temp_file" "$file"
done

# Rename mastra-cloud directory to kastrax-cloud
if [ -d "kastrax-doc/src/content/en/docs/mastra-cloud" ]; then
    echo "Renaming mastra-cloud directory to kastrax-cloud"
    mkdir -p kastrax-doc/src/content/en/docs/kastrax-cloud
    
    # Move files from mastra-cloud to kastrax-cloud
    for file in kastrax-doc/src/content/en/docs/mastra-cloud/*.mdx; do
        basename=$(basename "$file")
        cat "$file" | sed 's/mastra/kastrax/g; s/Mastra/Kastrax/g' > "kastrax-doc/src/content/en/docs/kastrax-cloud/$basename"
        
        # Add implementation markers to the new file
        sed -i '' 's/^# \(.*\)$/# \1 ✅/' "kastrax-doc/src/content/en/docs/kastrax-cloud/$basename"
        sed -i '' 's/^## \(.*\)$/## \1 ✅/' "kastrax-doc/src/content/en/docs/kastrax-cloud/$basename"
    done
fi

echo "Documentation update complete!"
