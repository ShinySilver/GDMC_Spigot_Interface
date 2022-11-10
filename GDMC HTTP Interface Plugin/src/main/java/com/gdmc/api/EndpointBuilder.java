package com.gdmc.api;

import java.util.ArrayList;

import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.world.block.BlockState;

import spark.Response;
import spark.Route;
import spark.Spark;

public class EndpointBuilder {

	public interface EndpointFunction {
		public String run(Response res, Object[] input);
	}

	private enum Casts {
		CAST_INT, CAST_BLOCK, CAST_STRING
	}

	private String name;
	private ArrayList<Casts> casts;
	private ArrayList<String> names;
	private ArrayList<Object> defaultValues;

	public EndpointBuilder(String name) {
		this.name = name;
		this.casts = new ArrayList<>();
		this.names = new ArrayList<>();
		this.defaultValues = new ArrayList<>();
	}

	public EndpointBuilder addString(String name) {
		this.names.add(name);
		this.casts.add(Casts.CAST_STRING);
		this.defaultValues.add(null);
		return this;
	}

	public EndpointBuilder addOptionalString(String name, String defaultValue) {
		this.names.add(name);
		this.casts.add(Casts.CAST_STRING);
		this.defaultValues.add(defaultValue);
		return this;
	}

	public EndpointBuilder addInt(String name) {
		this.names.add(name);
		this.casts.add(Casts.CAST_INT);
		this.defaultValues.add(null);
		return this;
	}

	public EndpointBuilder addOptionalInt(String name, String defaultValue) {
		this.names.add(name);
		this.casts.add(Casts.CAST_INT);
		this.defaultValues.add(defaultValue);
		return this;
	}

	public EndpointBuilder addBlock(String name) {
		this.names.add(name);
		this.casts.add(Casts.CAST_BLOCK);
		this.defaultValues.add(null);
		return this;
	}

	public EndpointBuilder addOptionalBlock(String name, String defaultBlock) {
		this.names.add(name);
		this.casts.add(Casts.CAST_BLOCK);
		this.defaultValues.add(defaultBlock);
		return this;
	}

	public void put(EndpointFunction f) {
		Spark.put(name, genRoute(f));
	}

	public void get(EndpointFunction f) {
		Spark.get(name, genRoute(f));
	}

	public void post(EndpointFunction f) {
		Spark.post(name, genRoute(f));
	}

	private Route genRoute(EndpointFunction f) {
		return (req, res) -> {
			res.type("text/html");

			// The output array
			Object[] sanitizedParams = new Object[names.size()];

			// Checking that all non-optional attributes are set, and init the output array
			for (int i = 0; i < names.size(); i++) {
				if (req.queryParams(names.get(i)) == null) {
					if (defaultValues.get(i) != null) {
						sanitizedParams[i] = defaultValues.get(i);
					} else {
						res.status(400);
						return "Attribute \"" + names.get(i) + "\" must be set.";
					}
				} else {
					sanitizedParams[i] = req.queryParams(names.get(i));
				}
			}

			// Parsing & checking the sanity of all the attributes
			for (int i = 0; i < names.size(); i++) {
				switch (this.casts.get(i)) {
				case CAST_INT:
					try {
						sanitizedParams[i] = Integer.parseInt((String) sanitizedParams[i]);
					} catch (NumberFormatException e) {
						res.status(400);
						return "Attribute \"" + names.get(i) + "\" with value \"" + sanitizedParams[i]
								+ "\" cannot be parsed to an integer.";
					}
					break;
				case CAST_BLOCK:
					try {
						sanitizedParams[i] = BlockState.get((String) sanitizedParams[i]);
					} catch (InputParseException e) {
						res.status(400);
						return "Attribute \"" + names.get(i) + "\" with value \"" + sanitizedParams[i]
								+ "\" cannot be parsed to a BlockState.";
					}
					break;
				case CAST_STRING:
				}
			}

			// Running the callback with the sanitized array & returning
			res.status(200);
			return f.run(res, sanitizedParams);
		};
	}
}
