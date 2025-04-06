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

echo -e "\nDetailed reports saved in dependency-analysis/"
echo "Check dependency-analysis/maven-analysis.txt for Maven's dependency analysis"

# Add to README
cat << 'EOF' > dependency-analysis/DEPENDENCY.md
# Dependency Analysis

