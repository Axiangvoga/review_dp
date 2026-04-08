import sys
from pathlib import Path

# 直接运行本文件时 sys.path 只有 rag/，找不到上级目录的 utils；需把项目根加入路径
_AGENT_ROOT = Path(__file__).resolve().parent.parent
if str(_AGENT_ROOT) not in sys.path:
    sys.path.insert(0, str(_AGENT_ROOT))

from langchain_core.documents import Document
from utils.config_handler import chroma_conf
from langchain_community.vectorstores import FAISS

from model.factory import embed_model

from langchain_text_splitters import RecursiveCharacterTextSplitter
from utils.path_tool import get_abs_path
from utils.file_handler import pdf_load, text_load, listdir_with_allowed_type, get_file_md5
from utils.logger_handler import logger

import os


class VectorStoreService:
    def __init__(self):

        self.faiss_path = get_abs_path(chroma_conf["persist_directory"])

        os.makedirs(self.faiss_path, exist_ok=True)

        # 如果已有索引就加载，没有就先置为 None
        if os.path.exists(os.path.join(self.faiss_path, "index.faiss")):
            self.vector_store = FAISS.load_local(
                self.faiss_path,
                embed_model,
                allow_dangerous_deserialization=True
            )
        else:
            self.vector_store = None

        self.spliter = RecursiveCharacterTextSplitter(
            chunk_size=chroma_conf["chunk_size"],
            chunk_overlap=chroma_conf["chunk_overlap"],
            length_function=len,
            separators=chroma_conf["separators"],
        )

    def get_retriever(self):
        return self.vector_store.as_retriever(search_kwargs={"k": chroma_conf["k"]})

    def load_document(self):
        def check_md5(md5_for_check: str):
            if not os.path.exists(chroma_conf["md5_hex_store"]):
                open(get_abs_path(chroma_conf["md5_hex_store"]), "w", encoding="utf-8").close()
                return False  # 没有处理过
            with open(get_abs_path(chroma_conf["md5_hex_store"]), "r", encoding="utf-8") as f:
                for line in f.readlines():
                    if line.strip() == md5_for_check:
                        return True  # 处理过
                return False

        def save_md5(md5_for_check: str):
            with open(get_abs_path(chroma_conf["md5_hex_store"]), "a", encoding="utf-8") as f:
                f.write(md5_for_check + "\n")

        def get_file_document(read_path: str):  # 获取文件对象
            if read_path.endswith(".pdf"):
                return pdf_load(read_path)
            if read_path.endswith(".txt"):
                return text_load(read_path)
            return []

        allowed_files_path: list[str] = listdir_with_allowed_type(
            get_abs_path(chroma_conf["data_path"]),
            tuple(chroma_conf["allow_knowledge_file_type"])
        )
        for path in allowed_files_path:
            md5_hex = get_file_md5(path)
            if check_md5(md5_hex):
                logger.info(f"[加载知识库]{path}内容已经存在知识库内，跳过")
                continue
            try:
                documents = get_file_document(path)
                if not documents:
                    logger.warning(f"[加载知识库]{path}内没有有效文本内容，跳过")
                    continue
                split_document: list[Document] = self.spliter.split_documents(documents)
                if not split_document:
                    logger.warning(f"[加载知识库]{path}分片后没有有效文本内容，跳过")
                    continue

                if self.vector_store is None:
                    # 第一次创建索引
                    self.vector_store = FAISS.from_documents(split_document, embed_model)
                else:
                    # 追加到已有索引
                    self.vector_store.add_documents(split_document)

                    # 每次写入后保存到本地
                self.vector_store.save_local(self.faiss_path)

                save_md5(md5_hex)
                logger.info(f"成功加载数据库{path}内容")
            except Exception as e:
                # exc_info为True会记录详细的报错堆栈，如果为False仅记录报错信息本身
                logger.error(f"[加载知识库]{path}加载失败：{str(e)}", exc_info=True)
                continue


if __name__ == '__main__':
    vs = VectorStoreService()

    vs.load_document()

    retriever = vs.get_retriever()

    res = retriever.invoke("迷路")
    for r in res:
        print(r.page_content)
        print("-" * 20)
