#!/bin/bash
# dependency-check.sh

echo "=== Analyzing Direct Dependencies ==="

# Create a directory for analysis output
mkdir -p dependency-analysis

# Check Oracle JDBC usage
echo "Checking Oracle JDBC usage..."
find src/ -type f -name "*.java" | xargs grep -l "oracle.jdbc" > dependency-analysis/oracle-usage.txt

# Check Logging usage
echo "Checking SLF4J/Logback usage..."
find src/ -type f -name "*.java" | xargs grep -l "org.slf4j" > dependency-analysis/slf4j-usage.txt
find src/ -type f -name "*.java" | xargs grep -l "ch.qos.logback" > dependency-analysis/logback-usage.txt

# Check Test Framework usage
echo "Checking Test Framework usage..."
find src/ -type f -name "*.java" | xargs grep -l "org.junit" > dependency-analysis/junit-usage.txt
find src/ -type f -name "*.java" | xargs grep -l "org.mockito" > dependency-analysis/mockito-usage.txt

# Check HikariCP usage
echo "Checking HikariCP usage..."
find src/ -type f -name "*.java" | xargs grep -l "com.zaxxer.hikari" > dependency-analysis/hikari-usage.txt

# Check Picocli usage
echo "Checking Picocli usage..."
find src/ -type f -name "*.java" | xargs grep -l "picocli" > dependency-analysis/picocli-usage.txt

# Run Maven dependency analysis
echo "=== Running Maven Dependency Analysis ==="
mvn dependency:analyze > dependency-analysis/maven-analysis.txt

# Analyze required Java modules
echo "=== Analyzing Required Java Modules ==="
# Create a temporary file for module names
MODULES_FILE="dependency-analysis/java-modules.txt"
rm -f "$MODULES_FILE"

# Base modules (always needed)
echo "java.base" >> "$MODULES_FILE"

# Check for module requirements based on dependencies
if [ -s dependency-analysis/oracle-usage.txt ]; then
    echo "java.sql" >> "$MODULES_FILE"
    echo "java.naming" >> "$MODULES_FILE"
fi

if [ -s dependency-analysis/slf4j-usage.txt ] || [ -s dependency-analysis/logback-usage.txt ]; then
    echo "java.logging" >> "$MODULES_FILE"
fi

if [ -s dependency-analysis/picocli-usage.txt ]; then
    echo "java.desktop" >> "$MODULES_FILE"
fi

# Remove duplicates and sort
sort -u "$MODULES_FILE" -o "$MODULES_FILE"

# Parse and summarize results
echo "=== Summary ==="
echo "Dependencies found in code:"
for file in dependency-analysis/*-usage.txt; do
    if [ -s "$file" ]; then
        count=$(wc -l < "$file")
        name=$(basename "$file" -usage.txt)
        echo "- $name: Found in $count files"
    fi
done

echo -e "\nRequired Java modules:"
cat "$MODULES_FILE"

echo -e "\nDetailed reports saved in dependency-analysis/"
echo "Check dependency-analysis/maven-analysis.txt for Maven's dependency analysis"
echo "Check dependency-analysis/java-modules.txt for required Java modules"

# Add to README
cat << 'EOF' > dependency-analysis/DEPENDENCY.md
# Dependency Analysis

## Required Java Modules
The following Java modules are required based on the code analysis:

EOF
cat "$MODULES_FILE" >> dependency-analysis/DEPENDENCY.md
cat << 'EOF' >> dependency-analysis/DEPENDENCY.md

## Module Descriptions
- java.base: Core Java classes (always required)
- java.sql: Required for JDBC database access
- java.naming: Required for JNDI and naming services
- java.logging: Required for SLF4J/Logback logging
- java.desktop: Required for Picocli command-line interface

## Usage in jlink
When creating a custom JRE with jlink, include these modules using:
```bash
jlink --add-modules $(cat dependency-analysis/java-modules.txt | tr '\n' ',')
```
EOF

