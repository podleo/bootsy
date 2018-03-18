# bootsy
**bootsy** is a Kubernetes cluster bootstrapping operator. The primary goal is to simplify initial cluster initialization and configuration, as well as to simplify node construction and initialization. Bootsy will interact with various cloud providers as well as static inventory providers to manage node inventory and provision new inventory when required. 

## Master Bootstrapping
The primary object of the master boostrapping process is to initialize an etcd backend as well as the primary master processes including kube-apiserver, kube-controller-manager and kube-scheduler. All processes will run as containers on host networking, only requiring the installation of a docker runtime engine on the host. The isolation of the processes in containers will allow for a simplified upgrade procedure.

In addition to the primary process that will run on the master, weave networking as well as a simple dashboard will be deployed to the cluster. In the future, this bootstrapping mechanism can be enhanced to extend functions of the cluster programmatically installing desired applications into the cluster.

Finally, the Bootsy node management operator will be installed to allow for quick provisioning of additional node resources. In addition to the operator several custom resource definitions will be created in the system to support operations.

##  Operator
The goal of the operator is provide a simple resource driven way to manage cluster nodes. The operator will interact with several custom resource definitions created by the bootstrapping process.

### KubeNodeProvider
The KubeNodeProvider resource will act as the link between the underlying cloud provider, or existing infrastructure to determine what node resources exist. In cases when new nodes are required, the KubeNodeProvider resource will define how those new node resources are to be created. Each KubeNodeProvider will reference a type that will instruct the operator on which implementation class should be used to service the requests. The operator currently supports Azure and CenturyLink Cloud. Additional providers will be added in the future.

```yaml
apiVersion: bootsy.flyover.com/v1
kind: KubeNodeProvider
metadata:
  name: azure
spec:
  subscription: 3dd...
  client: d7f...
  tenant: 374...
  resourceGroup: USW2
  region: westus2
  network: USW2
  subnet: USW2
  credentialsSecret:
    name: azure
    namespace: bootsy
  type: azure
``` 

```yaml
apiVersion: v1
data:
  key: ...
kind: Secret
metadata:
  name: azure
  namespace: bootsy
type: Opaque
```

### KubeNodeController
The KubeNodeController utilizes the provider and label selectors to determine if the appropriate number of nodes have been provisioned. This is similar to a Deployment of ReplicationController, requiring that a certain number of nodes be provisioned at all times.

```yaml
apiVersion: bootsy.flyover.com/v1
kind: KubeNodeController
metadata:
  name: compute-nodes
spec:
  count: 4
  provider: azure
  selectors:
    type: compute
```

### KubeNode
The kubeNode is simply a reference specification for the operator. It will contain certain detail elements about the node, as well as a place for provider implementations to store specifics about the underlying resources. This specification will also container secret references that can be used by the operator to access the node for configuration.

```yaml
apiVersion: bootsy.flyover.com/v1
kind: KubeNode
metadata:
  creationTimestamp: 2018-02-18T22:48:08Z
  generateName: node-
  generation: 0
  labels:
    type: compute
  name: node-pn81m
  resourceVersion: "2352026"
  selfLink: /apis/bootsy.flyover.com/v1/node-pn81m
  uid: ca0e2cc2-14fd-11e8-9de1-000c293c5efd
spec:
  connector:
    authSecret:
      name: b833018b-525f-467d-b76f-0d70081d69ea
      namespace: bootsy
    type: ssh
  dockerReady: true
  instanceInfo:
    status: ready
  ipAddress: 10.0.1.15
  kubeletReady: true
  provider: azure
  type: node
```

## CenturyLink Cloud Provider
The CenturyLink Cloud (CLC) provider requires a few pieces of account specific detail like account alias, datacenter id, group id and network id. This provider also requires a credentials secret. This credentials secret should be composed of a username and password with enough access to provision resources at the requested location.

### Credentials Secret
The credentials secret is referenced by the operator via the provider specification. The secret specified in the provider should be accessible to the operator.

```yaml
apiVersion: v1
data:
  password: ..base64 encoded username
  username: ..base64 encoded password
kind: Secret
metadata:
  name: centurylink
  namespace: bootsy
type: Opaque
```

### CenturyLink Provider
The CenturyLink provider will be used by the operator to initiate all automation related to infrastructure for the nodes. This includes create operations, status checks and in the future delete operations. As you can see we have referenced the previously created secret below. Additionally, we have several fields related to the virtual machine instance like `cpu` and `memoryGB`.

```yaml
apiVersion: bootsy.flyover.com/v1
kind: KubeNodeProvider
metadata:
  name: centurylink
spec:
  credentialsSecret:
    name: centurylink
    namespace: bootsy
  accountAlias: WOPR
  datacenter: WA1
  group: ...group id
  cpu: 4
  memoryGB: 8
  network: ...network id
  os: UBUNTU-16-64-TEMPLATE
  type: centurylink
```