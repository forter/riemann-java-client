#!/bin/bash -e
sudo apt-get install build-essential
mkdir /tmp/protobuf_install
cd /tmp/protobuf_install
wget https://github.com/google/protobuf/releases/download/v2.5.0/protobuf-2.5.0.tar.gz
tar xzvf protobuf-2.5.0.tar.gz
cd  protobuf-2.5.0
./configure
make
make check
sudo make install
sudo ldconfig
protoc --version
