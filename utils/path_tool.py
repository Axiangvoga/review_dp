import os


def get_project_root():
    current_file = os.path.abspath(__file__)  # 当前文件的绝对路径
    project_root = os.path.dirname(os.path.dirname(current_file))  # 当前文件的父目录的父目录
    return project_root  # 项目根目录


def get_abs_path(relative_path):
    project_root = get_project_root()
    return os.path.join(project_root, relative_path)
