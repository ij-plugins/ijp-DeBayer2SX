# ijp-DeBayer2SX
Raw Bayer-pattern image to color image converters.

Demasaicing (Bayer patter reconstruction) algorithms implemented, all support 8 bit and 16 bit input:

* __DDFPD__ - published in: "Demosaicing with Directional Filtering and a Posteriori Decision", D. Menon, S. Andriani, and G. Calvagno, _IEEE Trans. Image Processing_, vol. 16 no. 1, Jan. 2007. Versions with and without refining are provided.
* __Copy__ - simple copying of color values to corresponding RGB bands, no reconstruction.
* __Replication__ - from [Debayer plugin][debayer]
* __Bilinear__ - from [Debayer plugin][debayer]
* __Smooth Hue__ - from [Debayer plugin][debayer]
* __Adaptive Smooth Hue__ - from [Debayer plugin][debayer]

## Project Structure
* __ijp-debayer2sx-plugins__ - the front-end ImageJ plugins for end-user.
* __ijp-debayer2sx-core__ - the backend implementation of the algorithms. Intended to be used as a library by a developer.

## ImageJ Plugins
Plugins install by default under "plugins" > "IJ-Plugins"
* __DeBayer2__ - convert Bayer-pattern image to color image using various algorithms.
* __Make Bayer__ - convert color image to a Bayer-pattern image.


[debayer]: http://umanitoba.ca/faculties/science/astronomy/jwest/plugins.html
