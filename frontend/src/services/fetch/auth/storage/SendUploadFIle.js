import axios from "axios";
import {API_FILES} from "../../../../UrlConstants.jsx";
import bytes from "bytes";

const MAX_FILE_SIZE = 500 * 1024 * 1024;
const MAX_TOTAL_SIZE = 500 * 1024 * 1024;

function formatSize(sizeInBytes) {
    return bytes(sizeInBytes, {unitSeparator: ' '});
}

export async function sendUpload(files, updateDownloadTask, updateTask, uploadTask, currPath) {
    if (import.meta.env.VITE_MOCK_FETCH_CALLS) {
        console.log("Mocked fetch call for upload file");
        updateTask(uploadTask, "completed", "Загружено");
        return;
    }

    for (const {file} of files) {
        if (file.size > MAX_FILE_SIZE) {
            updateTask(uploadTask, "error",
                `Файл "${file.name}" (${formatSize(file.size)}) превышает лимит ${formatSize(MAX_FILE_SIZE)}`);
            return;
        }
    }

    const totalSize = files.reduce((sum, {file}) => sum + file.size, 0);
    if (totalSize > MAX_TOTAL_SIZE) {
        updateTask(uploadTask, "error",
            `Общий размер загрузки (${formatSize(totalSize)}) превышает лимит ${formatSize(MAX_TOTAL_SIZE)}`);
        return;
    }

    console.log("Файлы загружен на фронт: ");
    console.log(files);

    const formData = new FormData();
    files.forEach(({file, path}) => {
        formData.append("files", file, path);
    })
    formData.append("path", currPath);

    try {
        console.log("Отправляем файлы на бэк ");

        const response = await axios.post(API_FILES, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
            withCredentials: true,
            onUploadProgress: (progressEvent) => {
                updateTask(uploadTask, "progress", "Загружаем... " + bytes(progressEvent.rate) + "/c");
                if (progressEvent.progress === 1) {
                    updateTask(uploadTask, "progress", "Сохраняем в хранилище...")
                }

                updateDownloadTask(uploadTask, progressEvent.progress * 100);
            },
        });

        if (response.status === 201) {
            updateTask(uploadTask, "completed", "Загружено");
        }
    } catch (error) {
        if (error.response) {
            if (error.response.status === 409) {
                updateTask(uploadTask, "error", "Файл/папка с таким именем уже существует!");
            } else if (error.response.status === 400) {
                const message = error.response.data?.message || "Ошибка загрузки";
                updateTask(uploadTask, "error", message);
            } else {
                updateTask(uploadTask, "error", "Ошибка при загрузке. Попробуйте позже");
            }
        } else {
            updateTask(uploadTask, "error", "Файл слишком большой или соединение прервано");
        }
    }
}
