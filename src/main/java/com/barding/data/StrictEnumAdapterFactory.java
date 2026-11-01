package com.barding.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * Reads enums in any case and throws on unknown values. Plain Gson silently turns an
 * unknown enum value into null, which only shows up later as a confusing "missing field".
 */
public final class StrictEnumAdapterFactory implements TypeAdapterFactory
{
	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
	{
		final Class<? super T> raw = type.getRawType();
		if (!raw.isEnum())
		{
			return null;
		}

		return (TypeAdapter<T>) new TypeAdapter<Enum>()
		{
			@Override
			public void write(JsonWriter out, Enum value) throws IOException
			{
				out.value(value == null ? null : value.name());
			}

			@Override
			public Enum read(JsonReader in) throws IOException
			{
				if (in.peek() == JsonToken.NULL)
				{
					in.nextNull();
					return null;
				}
				String path = in.getPath();
				String text = in.nextString();
				for (Object constant : raw.getEnumConstants())
				{
					if (((Enum) constant).name().equalsIgnoreCase(text))
					{
						return (Enum) constant;
					}
				}
				throw new JsonParseException("Unknown " + raw.getSimpleName() + " '" + text + "' at " + path
					+ ". Expected one of " + Arrays.toString(raw.getEnumConstants()));
			}
		};
	}
}
