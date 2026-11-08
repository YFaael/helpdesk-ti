@echo off
cd /d "%~dp0"
where docker >nul 2>nul
if errorlevel 1 (
  echo Docker nao encontrado. Instale e abra o Docker Desktop.
  pause
  exit /b 1
)
if not exist ".env" (
  echo Crie o arquivo .env a partir de .env.example antes de continuar.
  pause
  exit /b 1
)
docker compose up --build -d
if errorlevel 1 (
  echo Erro ao iniciar o sistema. Confira o Docker Desktop.
  pause
  exit /b 1
)
echo.
echo Abra http://localhost:8088 no navegador.
pause
