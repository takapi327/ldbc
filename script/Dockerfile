FROM continuumio/miniconda3

RUN mkdir -p /opt/workspace && \
  conda install -y --quiet \
  numpy  \
  pandas \
  matplotlib \
  seaborn \
  jupyter && \
  conda clean -i -t -y

WORKDIR /opt/workspace

EXPOSE 8888

CMD [ "jupyter", "notebook", "--notebook-dir=/opt/workspace", "--ip=*", "--port=8888", "--no-browser" ]
