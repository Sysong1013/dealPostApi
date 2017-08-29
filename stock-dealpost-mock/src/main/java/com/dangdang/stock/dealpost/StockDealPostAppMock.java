package com.dangdang.stock.dealpost;

import com.alibaba.dubbo.container.spring.SpringContainer;

import java.util.concurrent.ConcurrentLinkedQueue;

public class StockDealPostAppMock {
	public static void main(String[] args) {
		SpringContainer container = new SpringContainer();
		container.start();

		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException e) {
			return;
		}
	}
}
