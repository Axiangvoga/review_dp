import os
import logging
from utils.path_tool import get_abs_path
from datetime import datetime

LOG_ROOT = get_abs_path("logs")  # 日志根目录

os.makedirs(LOG_ROOT, exist_ok=True)  # 创建日志目录

DEFAULT_LOG_FORMAT = logging.Formatter(
    '%(asctime)s - %(name)s - %(levelname)s - %(filename)s:%(lineno)d - %(message)s'
)  # 默认日志格式


def get_logger(
        name: str = "agent",
        console_level: int = logging.INFO,
        file_level: int = logging.INFO,
        log_file=None,
) -> logging.Logger:

    logger = logging.getLogger(name)
    logger.setLevel(logging.DEBUG)

    if logger.hasHandlers():
        return logger  # 如果已经有处理器，则直接返回

    # 创建控制台处理器
    console_handler = logging.StreamHandler()  # 流处理器
    console_handler.setLevel(console_level)  # 设置级别
    console_handler.setFormatter(DEFAULT_LOG_FORMAT)  # 设置格式

    logger.addHandler(console_handler)  # 添加处理器

    if not log_file:  # 如果日志文件为空，则创建日志文件
        log_file = os.path.join(LOG_ROOT, f"{name}_{datetime.now().strftime('%Y-%m-%d')}.log")

    # 创建文件处理器
    file_handler = logging.FileHandler(log_file, encoding="utf-8")
    file_handler.setLevel(file_level)
    file_handler.setFormatter(DEFAULT_LOG_FORMAT)
    logger.addHandler(file_handler)
    return logger


logger = get_logger()  # 创建默认日志记录器


