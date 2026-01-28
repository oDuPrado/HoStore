#!/usr/bin/env pwsh
# ============================================
# Renomeador de arquivos de documentação HoStore
# Robusto contra encoding (sem acentos em path)
# ============================================

# Usa a pasta onde o script está localizado
$docPath = $PSScriptRoot

# Mapeamento: caminho relativo original -> caminho relativo novo
$renamingMap = @{

    # ─── VISÃO GERAL (01–09)
    'MD\01_POR_QUE_HOSTORE_EXISTE.md'     = 'MD\01_POR_QUE_HOSTORE_EXISTE.md'
    'MD\02_GUIA_DE_DECISAO.md'            = 'MD\02_GUIA_DE_DECISAO.md'
    'MD\03_RISCOS_TECNICOS_E_OPERACAO.md' = 'MD\03_RISCOS_TECNICOS_E_OPERACAO.md'
    'MD\04_GUIA_DO_CLIENTE_COMO_USAR.md'  = 'MD\04_GUIA_DO_CLIENTE_COMO_USAR.md'
    'MD\QUICK_START.md'                   = 'MD\05_QUICK_START.md'
    'MD\README.md'                        = 'MD\06_README_GERAL.md'
    'MD\Portfolio.md'                     = 'MD\07_PORTFOLIO.md'
    'MD\ENTREGA_FINAL.md'                 = 'MD\08_ENTREGA_FINAL.md'
    'MD\CONCLUSAO_DOCUMENTACAO.md'        = 'MD\09_CONCLUSAO_DOCUMENTACAO.md'

    # ─── ARQUITETURA (10–19)
    'MD\ARQUITETURA.md'                   = 'MD\10_ARQUITETURA_GERAL.md'
    'MD\README_COMPLETE.md'               = 'MD\11_ESTRUTURA_MODULOS.md'
    'MD\FUNCIONALIDADES_COMPLETAS.md'     = 'MD\12_FUNCIONALIDADES_COMPLETAS.md'
    'MD\CHECKLIST_IMPLEMENTACAO_NFCE.md'  = 'MD\13_CHECKLIST_IMPLEMENTACAO.md'
    'MD\CHECKLIST_PRE_DEPLOYMENT_NFCE.md' = 'MD\14_CHECKLIST_PRE_DEPLOYMENT.md'
    'MD\15_IMPLEMENTACAO_COMPLETA_NFCE.md'= 'MD\15_IMPLEMENTACAO_NFCE.md'
    'MD\16_PROXIMAS_ACOES_MANUTENCAO.md'  = 'MD\16_PROXIMAS_ACOES.md'
    'MD\INDICE_DOCUMENTACAO.md'           = 'MD\17_INDICE_DOCUMENTACAO.md'
    'MD\INDICE_NFCE.md'                   = 'MD\18_INDICE_NFCE.md'
    'MD\INDICE_PACOTE_CLIENTE.md'         = 'MD\19_INDICE_PACOTE_CLIENTE.md'

    # ─── NFC-e (20–29)
    'MD\00_SUMARIO_EXECUTIVO_NFCE.md'      = 'MD\20_SUMARIO_EXECUTIVO_NFCE.md'
    'MD\RESUMO_IMPLEMENTACAO_NFCE.md'      = 'MD\21_RESUMO_IMPLEMENTACAO_NFCE.md'
    'MD\IMPLEMENTACAO_NFCE_COMPLETA.md'    = 'MD\22_IMPLEMENTACAO_NFCE_COMPLETA.md'
    'MD\IMPLEMENTACAO_NFCE_STATUS.md'      = 'MD\23_IMPLEMENTACAO_NFCE_STATUS.md'
    'MD\IMPLEMENTACAO_NFE_NFCE.md'         = 'MD\24_IMPLEMENTACAO_NFE_NFCE.md'
    'MD\IMPLEMENTACAO_COMPLETO_SISTEMA_MIGRACAO_NFCE.md' = 'MD\25_MIGRACAO_NFCE_SISTEMA.md'
    'MD\MANUAL_USO_SISTEMA_NFCE.md'         = 'MD\26_MANUAL_USO_NFCE.md'
    'MD\GUIA_TESTES_MIGRACAO_NFCE.md'       = 'MD\27_GUIA_TESTES_NFCE.md'
    'MD\MATRIZ_REFERENCIA_NFCE.md'          = 'MD\28_MATRIZ_REFERENCIA_NFCE.md'
    'MD\QUICK_START_NFCE_TESTES.md'         = 'MD\29_QUICK_START_TESTES.md'

    # ─── O QUE FALTA (30–32)
    'MD\ANALISE_COMPLETA_PROJETO.md'        = 'MD\30_ANALISE_COMPLETA_PROJETO.md'
    'MD\RESUMO_O_QUE_FALTA.md'              = 'MD\31_RESUMO_O_QUE_FALTA.md'
    'MD\GUIA_TECNICO_IMPLEMENTACAO.md'      = 'MD\32_GUIA_TECNICO_IMPLEMENTACAO.md'

    # ─── INVENTÁRIOS (40–43)
    'MD\LISTA_ARQUIVOS_DOCUMENTACAO.md'     = 'MD\40_LISTA_ARQUIVOS.md'
    'MD\INVENTARIO_ARQUIVOS_NFCE.md'        = 'MD\41_INVENTARIO_ARQUIVOS_NFCE.md'
    'MD\RESUMO_EXECUTIVO_ENTREGA.md'        = 'MD\42_RESUMO_EXECUTIVO_ENTREGA.md'
    'MD\RESUMO_VISUAL_FINAL_NFCE.md'        = 'MD\43_RESUMO_VISUAL_FINAL_NFCE.md'
}

Write-Host "=== RENOMEADOR DOCUMENTACAO HoStore ===" -ForegroundColor Cyan
Write-Host "Base: $docPath"
Write-Host ""

$count = 0

foreach ($orig in $renamingMap.Keys) {
    $dest = $renamingMap[$orig]

    $srcPath  = Join-Path $docPath $orig
    $destPath = Join-Path $docPath $dest

    Write-Host "TESTANDO: $srcPath"

    if (Test-Path $srcPath) {
        Move-Item -Path $srcPath -Destination $destPath -Force
        Write-Host "OK  $orig -> $dest" -ForegroundColor Green
        $count++
    } else {
        Write-Host "NAO ENCONTRADO: $orig" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Arquivos renomeados: $count"
Write-Host "Finalizado."
