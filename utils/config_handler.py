import yaml
from utils.path_tool import get_abs_path


def load_rag_config(config_path: str = get_abs_path("config\\rag.yml"), encoding: str = "utf-8"):  # 加载rag配置文件
    with open(config_path, "r", encoding=encoding) as f:  # 打开配置文件
        config = yaml.load(f, Loader=yaml.FullLoader)  # 加载配置文件
    return config  # 返回配置


def load_chroma_config(config_path: str = get_abs_path("config\\chroma.yml"), encoding: str = "utf-8"):  # 加载chroma配置文件
    with open(config_path, "r", encoding=encoding) as f:  # 打开配置文件
        config = yaml.load(f, Loader=yaml.FullLoader)  # 加载配置文件
    return config  # 返回配置


def load_prompts_config(config_path: str = get_abs_path("config\\prompts.yml"), encoding: str = "utf-8"):  # 加载rag配置文件
    with open(config_path, "r", encoding=encoding) as f:  # 打开配置文件
        config = yaml.load(f, Loader=yaml.FullLoader)  # 加载配置文件
    return config  # 返回配置


def load_agent_config(config_path: str = get_abs_path("config\\agent.yml"), encoding: str = "utf-8"):  # 加载rag配置文件
    with open(config_path, "r", encoding=encoding) as f:  # 打开配置文件
        config = yaml.load(f, Loader=yaml.FullLoader)  # 加载配置文件
    return config  # 返回配置


rag_conf = load_rag_config()

chroma_conf = load_chroma_config()

prompts_conf = load_prompts_config()

agent_conf = load_agent_config()


