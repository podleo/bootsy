mkdir -p /usr/bin/k8s-$KUBERNETES_VERSION

cp /var/lib/k8s/kubernetes/server/kubernetes/server/bin/kubectl /usr/bin/k8s-$KUBERNETES_VERSION/kubectl
cp /var/lib/k8s/kubernetes/server/kubernetes/server/bin/kube-apiserver /usr/bin/k8s-$KUBERNETES_VERSION/kube-apiserver
cp /var/lib/k8s/kubernetes/server/kubernetes/server/bin/kube-scheduler /usr/bin/k8s-$KUBERNETES_VERSION/kube-scheduler
cp /var/lib/k8s/kubernetes/server/kubernetes/server/bin/kube-controller-manager /usr/bin/k8s-$KUBERNETES_VERSION/kube-controller-manager
cp /var/lib/k8s/kubernetes/server/kubernetes/server/bin/kubelet /usr/bin/k8s-$KUBERNETES_VERSION/kubelet
cp /var/lib/k8s/kubernetes/server/kubernetes/server/bin/kube-proxy /usr/bin/k8s-$KUBERNETES_VERSION/kube-proxy

ln -sf /usr/bin/k8s-$KUBERNETES_VERSION/kubectl /usr/bin/kubectl
ln -sf /usr/bin/k8s-$KUBERNETES_VERSION/kube-apiserver /usr/bin/kube-apiserver
ln -sf /usr/bin/k8s-$KUBERNETES_VERSION/kube-scheduler /usr/bin/kube-scheduler
ln -sf /usr/bin/k8s-$KUBERNETES_VERSION/kube-controller-manager /usr/bin/kube-controller-manager
ln -sf /usr/bin/k8s-$KUBERNETES_VERSION/kubelet /usr/bin/kubelet
ln -sf /usr/bin/k8s-$KUBERNETES_VERSION/kube-proxy /usr/bin/kube-proxy

mkdir -p /opt/cni/bin

cp -r /ctnr/cni/bin/* /opt/cni/bin
