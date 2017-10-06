/**
 * @author Solomon Ritzow
 *
 */
module ritzow.sandbox {
	exports ritzow.sandbox.world.entity;
	exports ritzow.sandbox.audio;
	exports ritzow.sandbox.protocol;
	exports ritzow.sandbox.client.input.handler;
	exports ritzow.sandbox.client.input.controller;
	exports ritzow.sandbox.world.block;
	exports ritzow.sandbox.world.item;
	exports ritzow.sandbox.world.component;
	exports ritzow.sandbox.client;
	exports ritzow.sandbox.client.world;
	exports ritzow.sandbox.util;
	exports ritzow.sandbox.client.ui;
	exports ritzow.sandbox.main;
	exports ritzow.sandbox.server;
	exports ritzow.sandbox.world;
	exports ritzow.sandbox.client.audio;
	exports ritzow.sandbox.client.graphics;
	exports ritzow.sandbox.client.input;

	requires PNGDecoder;
	requires org.lwjgl;
	requires org.lwjgl.glfw;
	requires org.lwjgl.openal;
	requires org.lwjgl.opengl;
}