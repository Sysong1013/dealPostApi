package com.dangdang.stock.dealpost;

import org.springframework.stereotype.Service;
import com.dangdang.stock.api.StockDealPostService;
import com.dangdang.stock.dealpost.dto.Order;
import com.dangdang.stock.dealpost.dto.ResponseDTO;

@Service("stockDealPostService")
public class StockDealPostServiceMock implements StockDealPostService {
	private final long uptime = System.nanoTime() / 1000000;
	public ResponseDTO postStock(Order order) {
//		try {
//			long now = System.nanoTime() / 1000000;
//			System.out.println(now - uptime);
//			Thread.sleep(0);
//		} catch (InterruptedException e) {
//			Thread.currentThread().interrupt();
//		} finally {
			return new ResponseDTO(0, "");
//		}
	}
}
