package com.sx.server0.entity.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseRes {

	// websocket用户名
	protected String user;
	// 节点编号
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected String nodeNum;
	// 消息状态
	protected ResStatus status;

}
