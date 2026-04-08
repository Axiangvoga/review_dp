import hashlib
import os

from langchain_community.document_loaders import PyPDFLoader, TextLoader
from langchain_core.documents import Document

from utils.logger_handler import logger


def get_file_md5(file_path):
    if not os.path.exists(file_path):
        logger.error(f"{file_path}文件不存在")
        return
    if not os.path.isfile(file_path):
        logger.error(f"{file_path}并非文件")

    md5_obj = hashlib.md5()
    chunk_size = 4096  # 4KB
    try:
        with open(file_path, 'rb') as f:
            while chunk := f.read(chunk_size):
                md5_obj.update(chunk)
            md5_hex = md5_obj.hexdigest()
        return md5_hex
    except Exception as e:
        logger.error(f"计算{file_path}md5失败，{str(e)}")
        return None


def listdir_with_allowed_type(path, allowed_types):  # 返回文件夹内的文件列表
    file_list = []
    if not os.path.isdir(path):
        logger.error(f"{path}不是文件夹")
        return allowed_types
    for f in os.listdir(path):
        if f.endswith(allowed_types):
            file_list.append(os.path.join(path, f))
    return tuple(file_list)


def pdf_load(filepath: str, passwd=None) -> list[Document]:
    return PyPDFLoader(filepath, passwd).load()


def text_load(filepath: str) -> list[Document]:
    return TextLoader(filepath, encoding='utf-8').load()
