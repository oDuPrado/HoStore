#!/bin/bash

# Limpa e prepara pastas
echo "🔧 Limpando build antigo..."
rm -rf out
mkdir -p out/classes

echo "📦 Compilando arquivos .java..."
# Compila os .java para .class
javac -d out/classes -cp "lib/*" $(find src -name "*.java")

echo "📚 Gerando JAR com manifest..."
# Gera o JAR com as classes compiladas e manifest
jar cfm out/HoStore.jar MANIFEST.MF -C out/classes .

echo "✅ JAR gerado com sucesso: out/HoStore.jar"
