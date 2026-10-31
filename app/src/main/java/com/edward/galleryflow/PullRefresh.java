package com.edward.galleryflow;
/** Release position, not peak drag distance, determines whether refresh is committed. */
final class PullRefresh {static boolean shouldRefresh(float releaseTravel,float threshold,boolean cancelled){return !cancelled&&threshold>0&&releaseTravel>=threshold;}}
