FROM python:3.11-slim

WORKDIR /app

# 필요 패키지 설치

# Java 설치 (KoNLPy용 - 한국어모델용)
RUN apt-get update && apt-get install -y default-jdk curl && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . /app

CMD ["python", "analysisTag.py"]
