HoStore – ERP para lojas de TCG
================================

1. REQUISITOS
   • Windows 10/11 64 bits
   • Java Runtime 17 ou superior
     Verifique abrindo o CMD e executando `java -version`.
     Caso não tenha, baixe em: https://adoptium.net/temurin/releases/

2. PRIMEIRA INSTALAÇÃO
   • Extraia o arquivo HoStore.zip em C:\HoStore (ou outra pasta de sua preferência).
   • Dê dois cliques em  start.bat  para abrir o sistema.
   • O banco de dados local fica em  data\hostore.db  — faça backup dessa pasta se desejar.

3. ATUALIZAÇÃO AUTOMÁTICA
   • Sempre que houver nova versão, o sistema baixa silenciosamente o arquivo HoStore-novo.jar.
   • Ao fechar e abrir novamente, a atualização é aplicada automaticamente.
   • Caso algo dê errado, o launcher mantém cópias dos JARs antigos em  backup\.

4. SUPORTE
   • Dúvidas ou problemas: suporte@hostore.com.br

5 - Conteúdo:
- HoStore.jar          → Sistema principal
- Atualizador.jar      → Atualizador automático
- start.bat            → Atalho que verifica e aplica atualizações automaticamente
- versao.properties    → Não alterar! Contém a versão local
- data/                → Base de dados e arquivos

