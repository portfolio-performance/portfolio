#!/bin/bash

# Script to move translation keys from UI messages files to core messages files
# Usage: ./move-translation-key.sh <key>

if [ $# -ne 1 ]; then
    echo "Usage: $0 <key>"
    echo "Example: $0 MyTranslationKey"
    exit 1
fi

KEY="$1"

# Define the base paths
UI_BASE_PATH="../../name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui"
CORE_BASE_PATH="../../name.abuchen.portfolio/src/name/abuchen/portfolio"

# Function to remove key from a file
remove_key_from_file() {
    local file="$1"
    local key="$2"
    
    if [ -f "$file" ]; then
        # Remove the line containing the key (case-sensitive, exact match)
        # Escape special characters in the key for sed
        escaped_key=$(echo "$key" | sed 's/[[\.*^$()+?{|]/\\&/g')
        sed -i.bak "/^${escaped_key}[ ]*=/d" "$file"
        rm -f "${file}.bak"
        echo "Removed key '$key' from $file"
    else
        echo "Warning: File $file not found"
    fi
}

# Function to add key to a file
add_key_to_file() {
    local file="$1"
    local key="$2"
    local value="$3"
    
    if [ -f "$file" ]; then
        # Check if key already exists
        escaped_key=$(echo "$key" | sed 's/[[\.*^$()+?{|]/\\&/g')
        if grep -q "^${escaped_key}[ ]*=" "$file"; then
            echo "Warning: Key '$key' already exists in $file"
        else
            # Add the key=value to the end of the file
            echo "${key}=${value}" >> "$file"
            echo "Added key '$key' to $file"
        fi
    else
        echo "Warning: File $file not found"
    fi
}

# Step 1: Extract the key-value pairs from UI messages files
echo "Step 1: Extracting key-value pairs from UI messages files..."

# Create temporary files to store translations
TRANSLATIONS_DIR="/tmp/move_key_$$"
mkdir -p "$TRANSLATIONS_DIR"

# Get all UI messages files
UI_FILES=(
    "$UI_BASE_PATH/messages.properties"
    "$UI_BASE_PATH/messages_cs.properties"
    "$UI_BASE_PATH/messages_da.properties"
    "$UI_BASE_PATH/messages_de.properties"
    "$UI_BASE_PATH/messages_es.properties"
    "$UI_BASE_PATH/messages_fr.properties"
    "$UI_BASE_PATH/messages_it.properties"
    "$UI_BASE_PATH/messages_nl.properties"
    "$UI_BASE_PATH/messages_pl.properties"
    "$UI_BASE_PATH/messages_pt.properties"
    "$UI_BASE_PATH/messages_pt_BR.properties"
    "$UI_BASE_PATH/messages_ru.properties"
    "$UI_BASE_PATH/messages_sk.properties"
    "$UI_BASE_PATH/messages_tr.properties"
    "$UI_BASE_PATH/messages_vi.properties"
    "$UI_BASE_PATH/messages_zh.properties"
    "$UI_BASE_PATH/messages_zh_TW.properties"
)

# Extract key-value pairs from each UI file
found_key=false
for file in "${UI_FILES[@]}"; do
    if [ -f "$file" ]; then
        # Match key with optional spaces around the equals sign
        value=$(grep "^${KEY}[ ]*=" "$file" | cut -d'=' -f2- | sed 's/^[ ]*//')
        if [ -n "$value" ]; then
            ui_filename=$(basename "$file")
            # Convert UI filename to CORE filename pattern (messages -> bundle)
            core_filename=$(echo "$ui_filename" | sed 's/^messages/bundle/')
            echo "$value" > "$TRANSLATIONS_DIR/$core_filename"
            echo "Found in $ui_filename: $KEY=$value"
            found_key=true
        fi
    fi
done

# Check if key was found in any file
if [ "$found_key" = false ]; then
    echo "Error: Key '$KEY' not found in any UI messages files"
    rm -rf "$TRANSLATIONS_DIR"
    exit 1
fi

# Step 2: Remove key from UI messages files
echo "Step 2: Removing key from UI messages files..."

for file in "${UI_FILES[@]}"; do
    remove_key_from_file "$file" "$KEY"
done

# Step 3: Add key to core messages files
echo "Step 3: Adding key to core messages files..."

# Get all core messages files
CORE_FILES=(
    "$CORE_BASE_PATH/messages.properties"
    "$CORE_BASE_PATH/messages_cs.properties"
    "$CORE_BASE_PATH/messages_da.properties"
    "$CORE_BASE_PATH/messages_de.properties"
    "$CORE_BASE_PATH/messages_es.properties"
    "$CORE_BASE_PATH/messages_fr.properties"
    "$CORE_BASE_PATH/messages_it.properties"
    "$CORE_BASE_PATH/messages_nl.properties"
    "$CORE_BASE_PATH/messages_pl.properties"
    "$CORE_BASE_PATH/messages_pt.properties"
    "$CORE_BASE_PATH/messages_pt_BR.properties"
    "$CORE_BASE_PATH/messages_ru.properties"
    "$CORE_BASE_PATH/messages_sk.properties"
    "$CORE_BASE_PATH/messages_tr.properties"
    "$CORE_BASE_PATH/messages_vi.properties"
    "$CORE_BASE_PATH/messages_zh.properties"
    "$CORE_BASE_PATH/messages_zh_TW.properties"
)

# Add key-value pairs to corresponding core files
for file in "${CORE_FILES[@]}"; do
    filename=$(basename "$file")
    if [ -f "$TRANSLATIONS_DIR/$filename" ]; then
        value=$(cat "$TRANSLATIONS_DIR/$filename")
        add_key_to_file "$file" "$KEY" "$value"
    else
        echo "Warning: No translation found for $filename"
    fi
done

# Clean up temporary directory
rm -rf "$TRANSLATIONS_DIR"

echo "Key migration completed successfully!"