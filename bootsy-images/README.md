# Base Image build Automation

## Create runtime using docker container

Using a docker container as a runtime here to automate dependency installations and standardize build environment.

```
docker run -ti --rm -v /Users/mramach/projects_001/bootsy:/work -v /var/run:/var/run geerlingguy/docker-ubuntu1604-ansible /bin/bash
```

## Execute build for kubernetes version

```
root@a42e0cce98cd:/# cd /work/bootsy-images
root@a42e0cce98cd:/work/bootsy-images# ansible-playbook kube-base.yaml -e kube_version=v1.11.6
```
