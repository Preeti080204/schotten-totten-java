/**
 * Saving and resuming games as versioned JSON files.
 *
 * <p>The state is mapped to plain data transfer records ({@link
 * com.schottentotten.persistence.SaveFileDto}) and written with Jackson; no Java object
 * serialisation is used. Loading checks the format version and the consistency of every card
 * (each card exactly once), and reports problems as a
 * {@link com.schottentotten.persistence.SaveFormatException} with a message suitable for the
 * player.</p>
 */
package com.schottentotten.persistence;
