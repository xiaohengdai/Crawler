#!/bin/bash

#java -jar target/kl_crawler-2.0.jar -f config.yml -t 4723  -u 00008020-000509D4266A002E
java -jar target/kl_crawler-2.0.jar -f config_delivery.yml -t 4723  -u Z9K7ONN799999999
#java -jar target/kl_crawler-2.0.jar -f config.yml -t 4723  -u 127.0.0.1:62001
#java -jar target/kl_crawler-2.0.jar -f config.yml -t 4723  -u 57aff9d7339e56fcb36e1a49929cde55b7d78578
#java -jar /Users/xiaoheng/Downloads/meituan/UICrawler/target/UICrawler-2.0.jar -f /Users/xiaoheng/Downloads/meituan/UICrawler/config.yml -t 4723  -u 172.18.92.94:44401



#appium --session-override    -p 设定appium server的端口 , 不加参数默认为4723