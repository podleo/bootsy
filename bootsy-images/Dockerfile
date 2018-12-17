FROM ubuntu:16.04

RUN apt-get update; apt-get install wget -y; apt-get install iproute2 -y;

ENV KUBERNETES_SKIP_CONFIRM=true
ENV KUBERNETES_VERSION=v1.11.0

RUN mkdir -p /var/lib/k8s/kubernetes/server/kubernetes/server/bin
RUN mkdir -p /ctnr/cni/bin

RUN wget https://github.com/containernetworking/cni/releases/download/v0.5.2/cni-amd64-v0.5.2.tgz
RUN tar -xvzf /cni-amd64-v0.5.2.tgz -C /ctnr/cni/bin

ADD kubernetes/server/kubernetes/server/bin/kube-apiserver /var/lib/k8s/kubernetes/server/kubernetes/server/bin/kube-apiserver
ADD kubernetes/server/kubernetes/server/bin/kube-controller-manager /var/lib/k8s/kubernetes/server/kubernetes/server/bin/kube-controller-manager
ADD kubernetes/server/kubernetes/server/bin/kube-scheduler /var/lib/k8s/kubernetes/server/kubernetes/server/bin/kube-scheduler
ADD kubernetes/server/kubernetes/server/bin/kubelet /var/lib/k8s/kubernetes/server/kubernetes/server/bin/kubelet
ADD kubernetes/server/kubernetes/server/bin/kube-proxy /var/lib/k8s/kubernetes/server/kubernetes/server/bin/kube-proxy
ADD kubernetes/server/kubernetes/server/bin/kubectl /var/lib/k8s/kubernetes/server/kubernetes/server/bin/kubectl

ENV PATH=$PATH:/var/lib/k8s/kubernetes/server/kubernetes/server/bin

ADD copy.sh copy.sh

ENTRYPOINT /bin/bash
