FROM ubuntu:20.04

ENV DEBIAN_FRONTEND="noninteractive" TZ="Europe/London"
RUN apt-get update
RUN export PATH=$HOME/.local/bin:$PATH
RUN apt-get install -y locales build-essential git default-jre maven vim
RUN sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen && \
    locale-gen
ENV LANG en_US.UTF-8  
ENV LANGUAGE en_US:en  
ENV LC_ALL en_US.UTF-8     

# # # INSTALL JavaSDG
RUN git clone https://kaz.dsic.upv.es/git/program-slicing/SDG.git

WORKDIR "/SDG"

RUN mvn package -Dmaven.test.skip
RUN mv ./sdg-cli/target/sdg-cli-1.3.0-jar-with-dependencies.jar ./javaSDG.jar

