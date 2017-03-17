package com.alibaba.fastjson.serializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.DefaultJSONParser.ResolveTask;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.ParseContext;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.fanyu.boundless.config.Preferences;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.lang.reflect.Type;

public class AwtCodec implements ObjectSerializer, ObjectDeserializer {
    public static final AwtCodec instance = new AwtCodec();

    public static boolean support(Class<?> clazz) {
        return clazz == Point.class || clazz == Rectangle.class || clazz == Font.class || clazz == Color.class;
    }

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.out;
        if (object == null) {
            out.writeNull();
            return;
        }
        if (object instanceof Point) {
            Point font = (Point) object;
            out.writeFieldValue(writeClassName(out, Point.class, '{'), "x", font.x);
            out.writeFieldValue(',', "y", font.y);
        } else if (object instanceof Font) {
            Font font2 = (Font) object;
            out.writeFieldValue(writeClassName(out, Font.class, '{'), Preferences.sbry, font2.getName());
            out.writeFieldValue(',', TtmlNode.TAG_STYLE, font2.getStyle());
            out.writeFieldValue(',', "size", font2.getSize());
        } else if (object instanceof Rectangle) {
            Rectangle rectangle = (Rectangle) object;
            out.writeFieldValue(writeClassName(out, Rectangle.class, '{'), "x", rectangle.x);
            out.writeFieldValue(',', "y", rectangle.y);
            out.writeFieldValue(',', "width", rectangle.width);
            out.writeFieldValue(',', "height", rectangle.height);
        } else if (object instanceof Color) {
            Color color = (Color) object;
            out.writeFieldValue(writeClassName(out, Color.class, '{'), "r", color.getRed());
            out.writeFieldValue(',', "g", color.getGreen());
            out.writeFieldValue(',', "b", color.getBlue());
            if (color.getAlpha() > 0) {
                out.writeFieldValue(',', "alpha", color.getAlpha());
            }
        } else {
            throw new JSONException("not support awt class : " + object.getClass().getName());
        }
        out.write(125);
    }

    protected char writeClassName(SerializeWriter out, Class<?> clazz, char sep) {
        if (!out.isEnabled(SerializerFeature.WriteClassName)) {
            return sep;
        }
        out.write(123);
        out.writeFieldName(JSON.DEFAULT_TYPE_KEY);
        out.writeString(clazz.getName());
        return ',';
    }

    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        JSONLexer lexer = parser.lexer;
        if (lexer.token() == 8) {
            lexer.nextToken(16);
            return null;
        } else if (lexer.token() == 12 || lexer.token() == 16) {
            T parsePoint;
            lexer.nextToken();
            if (type == Point.class) {
                parsePoint = parsePoint(parser, fieldName);
            } else if (type == Rectangle.class) {
                parsePoint = parseRectangle(parser);
            } else if (type == Color.class) {
                parsePoint = parseColor(parser);
            } else if (type == Font.class) {
                parsePoint = parseFont(parser);
            } else {
                throw new JSONException("not support awt class : " + type);
            }
            ParseContext context = parser.getContext();
            parser.setContext(parsePoint, fieldName);
            parser.setContext(context);
            return parsePoint;
        } else {
            throw new JSONException("syntax error");
        }
    }

    protected Font parseFont(DefaultJSONParser parser) {
        JSONLexer lexer = parser.lexer;
        int size = 0;
        int style = 0;
        String name = null;
        while (lexer.token() != 13) {
            if (lexer.token() == 4) {
                String key = lexer.stringVal();
                lexer.nextTokenWithColon(2);
                if (key.equalsIgnoreCase(Preferences.sbry)) {
                    if (lexer.token() == 4) {
                        name = lexer.stringVal();
                        lexer.nextToken();
                    } else {
                        throw new JSONException("syntax error");
                    }
                } else if (key.equalsIgnoreCase(TtmlNode.TAG_STYLE)) {
                    if (lexer.token() == 2) {
                        style = lexer.intValue();
                        lexer.nextToken();
                    } else {
                        throw new JSONException("syntax error");
                    }
                } else if (!key.equalsIgnoreCase("size")) {
                    throw new JSONException("syntax error, " + key);
                } else if (lexer.token() == 2) {
                    size = lexer.intValue();
                    lexer.nextToken();
                } else {
                    throw new JSONException("syntax error");
                }
                if (lexer.token() == 16) {
                    lexer.nextToken(4);
                }
            } else {
                throw new JSONException("syntax error");
            }
        }
        lexer.nextToken();
        return new Font(name, style, size);
    }

    protected Color parseColor(DefaultJSONParser parser) {
        JSONLexer lexer = parser.lexer;
        int r = 0;
        int g = 0;
        int b = 0;
        int alpha = 0;
        while (lexer.token() != 13) {
            if (lexer.token() == 4) {
                String key = lexer.stringVal();
                lexer.nextTokenWithColon(2);
                if (lexer.token() == 2) {
                    int val = lexer.intValue();
                    lexer.nextToken();
                    if (key.equalsIgnoreCase("r")) {
                        r = val;
                    } else if (key.equalsIgnoreCase("g")) {
                        g = val;
                    } else if (key.equalsIgnoreCase("b")) {
                        b = val;
                    } else if (key.equalsIgnoreCase("alpha")) {
                        alpha = val;
                    } else {
                        throw new JSONException("syntax error, " + key);
                    }
                    if (lexer.token() == 16) {
                        lexer.nextToken(4);
                    }
                } else {
                    throw new JSONException("syntax error");
                }
            }
            throw new JSONException("syntax error");
        }
        lexer.nextToken();
        return new Color(r, g, b, alpha);
    }

    protected Rectangle parseRectangle(DefaultJSONParser parser) {
        JSONLexer lexer = parser.lexer;
        int x = 0;
        int y = 0;
        int width = 0;
        int height = 0;
        while (lexer.token() != 13) {
            if (lexer.token() == 4) {
                int val;
                String key = lexer.stringVal();
                lexer.nextTokenWithColon(2);
                int token = lexer.token();
                if (token == 2) {
                    val = lexer.intValue();
                    lexer.nextToken();
                } else if (token == 3) {
                    val = (int) lexer.floatValue();
                    lexer.nextToken();
                } else {
                    throw new JSONException("syntax error");
                }
                if (key.equalsIgnoreCase("x")) {
                    x = val;
                } else if (key.equalsIgnoreCase("y")) {
                    y = val;
                } else if (key.equalsIgnoreCase("width")) {
                    width = val;
                } else if (key.equalsIgnoreCase("height")) {
                    height = val;
                } else {
                    throw new JSONException("syntax error, " + key);
                }
                if (lexer.token() == 16) {
                    lexer.nextToken(4);
                }
            } else {
                throw new JSONException("syntax error");
            }
        }
        lexer.nextToken();
        return new Rectangle(x, y, width, height);
    }

    protected Point parsePoint(DefaultJSONParser parser, Object fieldName) {
        JSONLexer lexer = parser.lexer;
        int x = 0;
        int y = 0;
        while (lexer.token() != 13) {
            if (lexer.token() == 4) {
                String key = lexer.stringVal();
                if (JSON.DEFAULT_TYPE_KEY.equals(key)) {
                    parser.acceptType("java.awt.Point");
                } else if ("$ref".equals(key)) {
                    return (Point) parseRef(parser, fieldName);
                } else {
                    int val;
                    lexer.nextTokenWithColon(2);
                    int token = lexer.token();
                    if (token == 2) {
                        val = lexer.intValue();
                        lexer.nextToken();
                    } else if (token == 3) {
                        val = (int) lexer.floatValue();
                        lexer.nextToken();
                    } else {
                        throw new JSONException("syntax error : " + lexer.tokenName());
                    }
                    if (key.equalsIgnoreCase("x")) {
                        x = val;
                    } else if (key.equalsIgnoreCase("y")) {
                        y = val;
                    } else {
                        throw new JSONException("syntax error, " + key);
                    }
                    if (lexer.token() == 16) {
                        lexer.nextToken(4);
                    }
                }
            } else {
                throw new JSONException("syntax error");
            }
        }
        lexer.nextToken();
        return new Point(x, y);
    }

    private Object parseRef(DefaultJSONParser parser, Object fieldName) {
        JSONLexer lexer = parser.getLexer();
        lexer.nextTokenWithColon(4);
        String ref = lexer.stringVal();
        parser.setContext(parser.getContext(), fieldName);
        parser.addResolveTask(new ResolveTask(parser.getContext(), ref));
        parser.popContext();
        parser.setResolveStatus(1);
        lexer.nextToken(13);
        parser.accept(13);
        return null;
    }

    public int getFastMatchToken() {
        return 12;
    }
}
