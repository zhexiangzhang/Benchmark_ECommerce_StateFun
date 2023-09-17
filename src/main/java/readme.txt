docker images

docker save -o Ecommerce.tar e-commerce-master-e-commerce-functions:latest


chmod +x docker-install.sh
./docker-install.sh

scp -P 2088 C:/Users/somniloquy/.ssh/id_rsa.pub ucloud@ssh.cloud.sdu.dk:/home/ucloud/.ssh
scp D:/Downloads/docker-compose.yml ucloud@130.225.38.155:/home/ucloud

# docker exec -it kafka kafka-console-consumer --topic hello --from-beginning --bootstrap-server kafka:29092

查看kafka所有topic
    docker exec -it kafka /bin/bash
    /usr/bin/kafka-topics --list --bootstrap-server kafka:29092
删除topic
    获取zookeeper的ip
        docker inspect -f "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}" zookeeper
    删除 （在kafka命令行）
        /usr/bin/kafka-topics --zookeeper <zookeeper IP>:2181 --delete --topic <topic name>
/usr/bin/kafka-topics --zookeeper 172.18.0.2:2181 --delete --topic addItemToCartTask
/usr/bin/kafka-topics --zookeeper 172.18.0.2:2181 --create --topic checkoutTask --partitions 1 --replication-factor 1
添加权限
    E:\KU\4 Pro\benchmarkDriver
    Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
    - deleteProductTask
    - updatePriceTask
    - addItemToCartTask
    - checkoutTask
    - updateDeliveryTask

collect metrics
    useful link:
        https://nightlies.apache.org/flink/flink-docs-master/docs/ops/metrics/#state-access-latency-tracking
        https://stackoverflow.com/questions/55016972/how-to-get-metrics-of-all-parallelisms-in-flink
    example
        http://localhost:8081/jobs/e0dfda4a2a271a1a3b448a9d3dd031c0/vertices/1ad2b25fbf60f6e7e3f1890441cd4d59
        http://localhost:8081/jobs/<jobID from dashboard>/vertices/<verticID>
        verticID can be get by looking at http://localhost:8081/jobs response
        (verticID 就是operatior感觉



curl -X PUT -H "Content-Type: application/vnd.e-commerce.types/UserLogin" -d '{"user_id": "1", "user_name": "Joe"}' localhost:8090/e-commerce.fns/login/1

host A : StateFunRun on VM  (EG. ssh ucloud@130.225.38.193)
host B : Driver run on Ubuntu  (EG. ssh ucloud@ssh.cloud.sdu.dk -p 2447)

0. 从windows上传 driver 到 host B 并解压
   scp -P <port of host B> D:/Downloads/EventBenchmark.zip ucloud@ssh.cloud.sdu.dk:/home/ucloud/
   unzip EventBenchmark.zip (在 /home/ucloud 下)
1. 在host B 中设置driver执行环境
   假设当前在 /home/ucloud 下cd
   1.1 进入提前上传好的工作目录
      cd ../../work/799718/
   1.2 安装dotnet环境
        chmod +x dotnet_setup.sh
      ./dotnet_setup.sh
   1.3 添加环境变量
        1.3.1 运行命令进入或创建.bash_profile文件
            nano ~/.bash_profile
        1.3.2 在文件中添加
            export PATH=$PATH:/home/ucloud/dotnet
        1.3.3 保存，退出并更新
            source ~/.bash_profile
        1.3.4 测试是否添加成功
            dotnet --version

1. host A 与 windows 用 ssh 连接 (host A 创建实例时上传 windows的 public key)
2. host B 中执行 ssh xx 任意命令 (如ssh ucloud@130.225.38.193) 以创建.ssh 文件夹，该文件夹在 /home/ucloud 下，为隐藏文件夹，使用 ls -a 查看
3. 将 windows .ssh文件夹中的 config, id_rsa, id_rsa.pub 上传到 host B 的 .ssh文件夹中
   scp -P <port of host B> C:/Users/somniloquy/.ssh/id_rsa.pub ucloud@ssh.cloud.sdu.dk:/home/ucloud/.ssh
   scp -P <port of host B> C:/Users/somniloquy/.ssh/id_rsa ucloud@ssh.cloud.sdu.dk:/home/ucloud/.ssh
   scp -P <port of host B> C:/Users/somniloquy/.ssh/config ucloud@ssh.cloud.sdu.dk:/home/ucloud/.ssh
4. 添加私钥使用权限
   chmod 600 ~/.ssh/id_rsa
5. 使用ssh在host B中连接 host A  (EG.)
   ssh ucloud@130.225.38.193
6. 验证成功后退出
   exit
7. 添加端口转发
   7.1 在 host B 打开ssh配置文件
       nano ~/.ssh/config
   7.2 在下方新加入以下配置信息，(host A的ssh连接方式为 ssh ucloud@130.225.38.193)
Host myhost
   HostName 130.225.38.193
   User ucloud
   LocalForward 8090 localhost:8090

        (本地主机8090端口上的接收的流量将通过SSH连接转发到远程主机上的端口号8090)
    7.3 保存并退出
    7.4 测试
        ssh myhost
        curl -X PUT -H "Content-Type: application/vnd.e-commerce.types/UserLogin" -d '{"user_id": "1", "user_name": "Joe"}' localhost:8090/e-commerce.fns/login/1
        (此时 host A 上的 StateFunRun应该会收到请求)ls
8. 运行driver
   8.1 进入benchmark文件夹
       cd /home/ucloud/EventBenchmark
   8.2 开启两个终端，分别运行
       dotnet run --project Silo
       dotnet run --project Client Configuration


提交代码:
    git status
    git add .
    git commit -m "message"
    git push origin statefun (for driver)
    git push origin main (for statefun)


git commit -m "kafka added"


5 为什么seller对status的数据和其他microservice (shipment and order)可以redentunt ==>【每次都会查询其他状态，所以用cache缓存这些状态，牺牲空间比失去顾客更重要】
6. queryDashboard 返回ok还是整体内容 【kafka会变得很慢，payload太大，用户等三秒就离开】

git commit -m "before change"