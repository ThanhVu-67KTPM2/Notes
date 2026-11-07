@echo off
chcp 65001 >nul
echo ==========================================
echo Thiet lap Git cho du an GhiChu (Kotlin)
echo ==========================================

:: Đặt biến môi trường cho thư mục và link GitHub
set "PROJECT_DIR=C:\Users\hi3ki\AndroidStudioProjects\GhiChu"
set "REPO_URL=https://github.com/ThanhVu-67KTPM2/Notes.git"

:: 1. Di chuyển tới thư mục dự án
echo [*] Dang truy cap thu muc: %PROJECT_DIR%
cd /d "%PROJECT_DIR%"
if %errorlevel% neq 0 (
    echo [Loi] Khong tim thay thu muc. Hay kiem tra lai duong dan!
    pause
    exit /b
)

:: Khởi tạo repo cục bộ nếu chưa có
if not exist .git (
    echo [*] Dang khoi tao Git repository (git init)...
    git init
) else (
    echo [*] Thu muc da co san .git.
)

:: 2. Tạo liên kết với GitHub
echo [*] Dang tao lien ket voi: %REPO_URL%
git remote add origin "%REPO_URL%" 2>nul
if %errorlevel% neq 0 (
    echo [*] Remote origin da ton tai. Dang cap nhat lai URL...
    git remote set-url origin "%REPO_URL%"
)

:: 3. Tạo và chuyển sang nhánh dev
echo [*] Dang tao nhanh 'dev'...
git checkout -b dev 2>nul
if %errorlevel% neq 0 (
    echo [*] Nhanh 'dev' co the da ton tai, dang chuyen sang nhanh dev...
    git checkout dev
)

echo ==========================================
echo [Thanh cong] Da lien ket GitHub va tao nhanh 'dev'!
echo ==========================================
pause