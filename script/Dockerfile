FROM python:3.9

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY plot_benchmark.py .

ENTRYPOINT ["python", "plot_benchmark.py"]
