# ☁️ Cloud File Storage

Облачное хранилище файлов с веб-интерфейсом. Поддерживает загрузку, скачивание, перемещение и удаление файлов и папок, поиск по хранилищу.

---

## 🛠️ Технологический стек

| Слой                   | Технологии                                    |
|------------------------|-----------------------------------------------|
| **Frontend**           | React 19, Vite 6, MUI 6, Axios, Framer Motion |
| **Backend**            | Java 21, Spring Boot                          |
| **База данных**        | PostgreSQL 16                                 |
| **Кэширование сессий** | Redis 8                                       |
| **Хранилище объектов** | MinIO (S3-совместимое)                        |
| **Контейнеризация**    | Docker                                        |

---

## 🚀 Локальный запуск (разработка)

### Требования

- [Docker](https://docs.docker.com/get-docker/) и Docker Compose
- [Node.js 22+](https://nodejs.org/)
- [JDK 21+](https://adoptium.net/) (для запуска бэкенда без Docker)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/) или другой IDE (опционально)

### 1. Клонировать репозиторий

```bash
git clone https://github.com/waynehays/cloud-file-storage.git
cd cloud-file-storage
```

### 2. Запустить инфраструктуру для разработки

`docker-compose.dev.yml` поднимает только зависимости (PostgreSQL, Redis, MinIO) с тестовыми учётными данными:

```bash
docker compose -f docker-compose.dev.yml up -d
```

| Сервис | Порт | Учётные данные |
|---|---|---|
| PostgreSQL | `55432` | `dev-user` / `dev-password` / БД `dev-db` |
| Redis | `56379` | — |
| MinIO API | `59000` | `minioadmin` / `minioadmin123` |
| MinIO Console | `59001` | `minioadmin` / `minioadmin123` |

### 3. Запустить бэкенд

```bash
cd backend
./gradlew bootRun
```

Или запустите через IntelliJ IDEA (`Run > cloud-file-storage`). Бэкенд поднимается на `http://localhost:8080`.

### 4. Запустить фронтенд

```bash
cd frontend
npm install
npm run dev
```

Приложение будет доступно на `http://localhost:5173`.

### 5. Остановить инфраструктуру

```bash
docker compose -f docker-compose.dev.yml down
```

Для полного удаления данных:

```bash
docker compose -f docker-compose.dev.yml down -v
```

---

## 🌐 Деплой на сервер

### Требования

- Сервер с Docker
- Открытый порт `80`
- Доступ к `ghcr.io`

### 1. Подготовить сервер

```bash
# Установить Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker
```

### 2. Склонировать репозиторий на сервер

```bash
git clone https://github.com/waynehays/cloud-file-storage.git
cd cloud-file-storage
```

### 3. Создать файл переменных окружения

Создайте файл `.env` в корне проекта:

```bash
nano .env
```

Заполните следующие переменные:

```dotenv
# PostgreSQL
POSTGRES_USER=your_db_user
POSTGRES_PASSWORD=your_strong_db_password
POSTGRES_DB=db_name

# MinIO (S3-совместимое хранилище)
MINIO_ACCESS_KEY=your_minio_access_key
MINIO_SECRET_KEY=your_minio_secret

# Grafana
GRAFANA_USER=admin
GRAFANA_PASSWORD=your_grafana_password
```

### 4. Запустить все сервисы

```bash
docker compose up -d
```

Docker Compose автоматически:
1. Скачает образы `backend` и `frontend` с `ghcr.io`
2. Запустит зависимости (PostgreSQL, Redis, MinIO) с healthcheck-ами
3. Дождётся их готовности перед стартом бэкенда
4. Поднимет фронтенд только после того, как бэкенд пройдёт healthcheck

### 5. Проверить состояние

```bash
# Статус всех контейнеров
docker compose ps

# Логи конкретного сервиса
docker compose logs -f backend
docker compose logs -f frontend
```

Приложение будет доступно на `http://<IP-сервера>`.

### Порты и доступ

| Сервис | Адрес | Описание |
|---|---|---|
| Приложение | `http://server-ip:80` | Основной интерфейс |
| Grafana | `http://server-ip:3000` | Мониторинг (только localhost) |
| Prometheus | `http://server-ip:9090` | Метрики (только localhost) |
| Swagger UI | `http://server-ip/swagger-ui/` | Документация API |

> Grafana и Prometheus по умолчанию привязаны к `127.0.0.1` — не доступны извне без SSH-туннеля или настройки прокси.

### 6. Обновление до новой версии

```bash
git pull
docker compose pull
docker compose up -d
```

### 7. Полная остановка

```bash
# Остановить без удаления данных
docker compose down

# Остановить и удалить все данные (НЕОБРАТИМО)
docker compose down -v
```
