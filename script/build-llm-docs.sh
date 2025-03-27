#!/bin/bash

# Directory paths
DOCS_DIR="docs/target/mdoc/en"
TUTORIAL_DOCS_DIR="docs/target/mdoc/en/tutorial"
QA_DOCS_DIR="docs/target/mdoc/en/qa"
EXAMPLES_DOCS_DIR="docs/target/mdoc/en/examples"
REFERENCE_DOCS_DIR="docs/target/mdoc/en/reference"
PUBLIC_DIR="docs/target/docs/site"

# Function to remove file extension
slice_ext() {
  echo "$1" | sed 's/\.[^.]*$//'
}

# Function to extract label from filename
extract_label() {
  filename=$(basename "$1")
  slice_ext "$filename"
}

# Function to capitalize words separated by hyphens
capitalize_delimiter() {
  local str=$1
  local result=""

  # Split string by hyphens and capitalize each part
  IFS='-' read -ra PARTS <<< "$str"
  for i in "${!PARTS[@]}"; do
    part=${PARTS[$i]}
    if [ -n "$part" ]; then
      capitalized="$(tr '[:lower:]' '[:upper:]' <<< ${part:0:1})${part:1}"
      result+="$capitalized"
      if [ $i -lt $((${#PARTS[@]} - 1)) ]; then
        result+="-"
      fi
    fi
  done

  echo "$result"
}

# Function to remove frontmatter from markdown content
remove_frontmatter() {
  sed -E '/^---(\n|.)*?^---$/d' "$1"
}

# Generate llms.txt file
generate_llms_txt() {
  OUTPUT_FILE="$PUBLIC_DIR/llms.txt"

  # Create header content
  cat > "$OUTPUT_FILE" << EOL
# ldbc

> ldbc - ldbc (Lepus Database Connectivity) is Pure functional JDBC layer with Cats Effect 3 and Scala 3.

## Docs

- [Full Docs](https://takapi327.github.io/ldbc/llms-full.txt) Full documentation of ldbc. (without examples)
- [Tiny Docs](https://takapi327.github.io/ldbc/llms-small.txt): Tiny documentation of ldbc. (includes only desciption of core)

EOL

  # Add docs links from root directory
  echo "### Core" >> "$OUTPUT_FILE"
  find "$DOCS_DIR" -maxdepth 1 -name "*.md" | sort | while read -r file; do
    relative_path=${file#"$DOCS_DIR/"}
    label=$(extract_label "$relative_path")
    capitalized=$(capitalize_delimiter "$label")
    formatted=${capitalized//-/ }
    echo "- [$formatted](https://takapi327.github.io/ldbc/latest/$(slice_ext "$relative_path"))" >> "$OUTPUT_FILE"
  done

  # Add Tutorial docs links
  echo -e "\n### Tutorial" >> "$OUTPUT_FILE"
  find "$TUTORIAL_DOCS_DIR" -name "*.md" | sort | while read -r file; do
    relative_path=${file#"$TUTORIAL_DOCS_DIR/"}
    label=$(extract_label "$relative_path")
    capitalized=$(capitalize_delimiter "$label")
    formatted=${capitalized//-/ }
    echo "- [$formatted](https://takapi327.github.io/ldbc/latest/$(slice_ext "$relative_path"))" >> "$OUTPUT_FILE"
  done

  # Add QA docs links
  echo -e "\n### QA" >> "$OUTPUT_FILE"
  find "$QA_DOCS_DIR" -name "*.md" | sort | while read -r file; do
    relative_path=${file#"$QA_DOCS_DIR/"}
    label=$(extract_label "$relative_path")
    capitalized=$(capitalize_delimiter "$label")
    formatted=${capitalized//-/ }
    echo "- [$formatted](https://takapi327.github.io/ldbc/latest/$(slice_ext "$relative_path"))" >> "$OUTPUT_FILE"
  done

  # Add Examples docs links
  echo -e "\n## Examples" >> "$OUTPUT_FILE"
  find "$EXAMPLES_DOCS_DIR" -name "*.md" | sort | while read -r file; do
    relative_path=${file#"$EXAMPLES_DOCS_DIR/"}
    label=$(extract_label "$relative_path")
    capitalized=$(capitalize_delimiter "$label")
    formatted=${capitalized//-/ }
    echo "- [$formatted](https://takapi327.github.io/ldbc/latest/$(slice_ext "$relative_path"))" >> "$OUTPUT_FILE"
  done

  # Add Reference docs links
  echo -e "\n### Reference" >> "$OUTPUT_FILE"
  find "$REFERENCE_DOCS_DIR" -name "*.md" | sort | while read -r file; do
    relative_path=${file#"$REFERENCE_DOCS_DIR/"}
    label=$(extract_label "$relative_path")
    capitalized=$(capitalize_delimiter "$label")
    formatted=${capitalized//-/ }
    echo "- [$formatted](https://takapi327.github.io/ldbc/latest/$(slice_ext "$relative_path"))" >> "$OUTPUT_FILE"
  done

  echo "< Output '$OUTPUT_FILE'"
}

# Function to generate content for full and small docs
generate_content() {
  local output_file=$1
  local header=$2
  local exclude_pattern=$3

  echo -e "$header\n# Start of ldbc documentation" > "$output_file"

  if [ -z "$exclude_pattern" ]; then
    # Find all .md files
    find_cmd="find \"$DOCS_DIR\" -name \"*.md\""
  else
    # Find .md files but exclude specified directories
    find_cmd="find \"$DOCS_DIR\" -name \"*.md\" | grep -v -E \"$exclude_pattern\""
  fi

  eval "$find_cmd" | sort | while read -r file; do
    echo "> Writing '$file'"
    # First remove frontmatter (between --- markers)
    # Then remove content within {% %} tags with a more robust pattern
    cat "$file" | 
      sed -e '/^---$/,/^---$/d' | 
      awk '{
        if (match($0, /\{%/)) {
          in_tag = 1
        }
        if (!in_tag) {
          print
        }
        if (match($0, /%\}/)) {
          in_tag = 0
        }
      }' >> "$output_file"
    echo -e "\n" >> "$output_file"
  done

  echo "< Output '$output_file'"
}

# Ensure output directory exists
mkdir -p "$PUBLIC_DIR"

# Generate all files
generate_llms_txt

# Generate full documentation
generate_content "$PUBLIC_DIR/llms-full.txt" "<SYSTEM>This is the full developer documentation for ldbc.</SYSTEM>" ""

# Generate tiny documentation (excluding specified directories)
generate_content "$PUBLIC_DIR/llms-small.txt" "<SYSTEM>This is the tiny developer documentation for ldbc.</SYSTEM>" "/(concepts|helpers|middleware)/"
