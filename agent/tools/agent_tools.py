import os
from utils.logger_handler import logger
from langchain_core.tools import tool

from rag.rag_service import RagSummarizeService
import random
from utils.config_handler import agent_conf
from utils.path_tool import get_abs_path
import requests
from langchain_core.tools import tool
 
# Java 服务地址，和 agent 同配置文件管理
JAVA_BASE_URL = "http://localhost:8081"

rag = RagSummarizeService()

user_ids = ["1001", "1002", "1003", "1004", "1005", "1006", "1007", "1008", "1009", "1010", ]
month_arr = ["2025-01", "2025-02", "2025-03", "2025-04", "2025-05", "2025-06",
             "2025-07", "2025-08", "2025-09", "2025-10", "2025-11", "2025-12", ]

external_data = {}


@tool(description="从向量存储中检索参考资料")
def rag_summarize(query: str) -> str:
    return rag.rag_summarize(query)


@tool(description="获取指定城市的天气，以消息字符串的形式返回")
def get_weather(city: str) -> str:
    return f"城市{city}天气为晴天，气温26摄氏度，空气湿度50%，南风1级，AQI21，最近6小时降雨概率极低"




@tool(description="获取用户的ID，以纯字符串形式返回")
def get_user_id() -> str:
    return random.choice(user_ids)


@tool(description="获取当前月份，以纯字符串形式返回")
def get_current_month() -> str:
    return random.choice(month_arr)


def generate_external_data():
    """
    {
        "user_id": {
            "month" : {"特征": xxx, "效率": xxx, ...}
            "month" : {"特征": xxx, "效率": xxx, ...}
            "month" : {"特征": xxx, "效率": xxx, ...}
            ...
        },
        "user_id": {
            "month" : {"特征": xxx, "效率": xxx, ...}
            "month" : {"特征": xxx, "效率": xxx, ...}
            "month" : {"特征": xxx, "效率": xxx, ...}
            ...
        },
        "user_id": {
            "month" : {"特征": xxx, "效率": xxx, ...}
            "month" : {"特征": xxx, "效率": xxx, ...}
            "month" : {"特征": xxx, "效率": xxx, ...}
            ...
        },
        ...
    }
    :return:
    """
    if not external_data:
        external_data_path = get_abs_path(agent_conf["external_data_path"])

        if not os.path.exists(external_data_path):
            raise FileNotFoundError(f"外部数据文件{external_data_path}不存在")

        with open(external_data_path, "r", encoding="utf-8") as f:
            for line in f.readlines()[1:]:
                arr: list[str] = line.strip().split(",")

                user_id: str = arr[0].replace('"', "")
                feature: str = arr[1].replace('"', "")
                efficiency: str = arr[2].replace('"', "")
                consumables: str = arr[3].replace('"', "")
                comparison: str = arr[4].replace('"', "")
                time: str = arr[5].replace('"', "")

                if user_id not in external_data:
                    external_data[user_id] = {}

                external_data[user_id][time] = {
                    "特征": feature,
                    "效率": efficiency,
                    "耗材": consumables,
                    "对比": comparison,
                }


@tool(description="从外部系统中获取指定用户在指定月份的使用记录，以纯字符串形式返回， 如果未检索到返回空字符串")
def fetch_external_data(user_id: str, month: str) -> str:
    generate_external_data()

    try:
        return external_data[user_id][month]
    except KeyError:
        logger.warning(f"[fetch_external_data]未能检索到用户：{user_id}在{month}的使用记录数据")
        return ""


@tool(description="无入参，无返回值，调用后触发中间件自动为报告生成的场景动态注入上下文信息，为后续提示词切换提供上下文信息")
def fill_context_for_report():
    return "fill_context_for_report已调用"


@tool(description="获取用户所在城市的名称，以纯字符串形式返回")
def get_user_location() -> str:
    print("调用get_user_location()工具")
    amap_key = "a2162f390b1afe5255d3a400e6b23c7a" 
    amap_url = f"https://restapi.amap.com/v3/ip?key={amap_key}&output=json"
    response = requests.get(amap_url);
    print("get_user_location()调用成功")
    if response.status_code == 200:
        data = response.json()
        print(f"API返回的原始数据: {data}")
        if data.get('status') == '1':
            province = data.get('province', '未知省份')
            city = data.get('city', '未知城市')
            rect = data.get('rectangle') # 格式: "x1,y1;x2,y2"
            
            location_info = f"城市：{province}-{city}"
            
            # 解析经纬度
            if rect and isinstance(rect, str):
                try:
                    # 拆分矩形坐标点
                    points = rect.split(';')
                    p1 = list(map(float, points[0].split(',')))
                    p2 = list(map(float, points[1].split(',')))
                    
                    # 计算中心点坐标 (经度lng, 纬度lat)
                    center_lng = round((p1[0] + p2[0]) / 2, 6)
                    center_lat = round((p1[1] + p2[1]) / 2, 6)
                    
                    location_info += f"，中心经纬度：{center_lng},{center_lat}"
                except Exception:
                    location_info += "，坐标解析失败"
            
            return location_info
        else:
            return f"定位失败：{data.get('info', '未知错误')}"

    return "网络请求失败，无法获取位置"

 
@tool(description="""根据用户的经纬度坐标，
查询指定半径内的餐厅列表并返回，供 Agent 进行推荐。参数说明：- longitude: 用户经度，如 120.1466- latitude:  用户纬度，如 30.312
- radius:    查询半径，默认 5
- unit:      单位 km 或 miles，默认 km
- type_id:   商户类型ID，不传则查全部类型
""")
def get_nearby_shops(
        longitude: float = 120.1466,
        latitude: float =30.312,
        radius: float = 5.0,
        unit: str = "km",
        type_id: int = 1
) -> str:
    try:
        print("调用get_nearby_shops工具")
        params = {
            "longitude": longitude,
            "latitude": latitude,
            "radius": radius,
            "unit": unit,
        }
        if type_id:
            params["typeId"] = type_id
        print("准备发送请求")
        resp = requests.get(
            f"{JAVA_BASE_URL}/shop/nearby",
            params=params,
            timeout=10
        )
        # 添加这两行打印！
        print(f"Python 视角 - 状态码: {resp.status_code}")
        print(f"Python 视角 - 响应体: {resp.text}")
        print("发送请求")
        resp.raise_for_status()
        data = resp.json()
        print(f"Java接口返回原始数据: {data}") # 加这一行
        # Java 返回格式: {"success": true, "data": [...]}
        if not data.get("success"):
            return f"查询失败：{data.get('errorMsg', '未知错误')}"
 
        shops = data.get("data", [])
        if not shops:
            return f"在您附近 {radius}{unit} 内未找到商户"
 
        # 格式化给 Agent 看的文本
        result = f"在您附近 {radius}{unit} 内找到 {len(shops)} 家商户：\n"
        for i, shop in enumerate(shops, 1):
            result += (
                f"【{i}】{shop.get('name', '未知')} "
                f"| 距离：{shop.get('distance', '未知')} "
                f"| 类型：{shop.get('typeId', '')} "
                f"| 评分：{shop.get('score', '暂无')} "
                f"| 地址：{shop.get('address', '未知')}\n"
            )
        return result
 
    except requests.Timeout:
        return "查询超时，请稍后重试"
    except Exception as e:
        return f"查询附近商户失败：{str(e)}"

if __name__ == '__main__':
    res = (fetch_external_data("1001", "2025-03"))
    print(res)
